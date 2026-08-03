#!/usr/bin/env bash
# eNurse production deploy — BT Panel (宝塔) variant. Run on the server:
#   bash deploy/scripts/deploy.sh
#
# Prereqs (see docs/deploy.md):
#   1. BT Panel installed; site created for the domain, root = /www/wwwroot/enurse
#   2. deploy/nginx/bt-site.conf pasted into the site config; SSL applied in BT
#   3. Docker installed; deploy/.env.prod filled in
# Override site root with:  WEB_ROOT=/www/wwwroot/other bash deploy/scripts/deploy.sh
set -euo pipefail

cd "$(dirname "$0")/../.."   # repo root
DEPLOY_DIR="deploy"
ENV_FILE="$DEPLOY_DIR/.env.prod"
WEB_ROOT="${WEB_ROOT:-/www/wwwroot/enurse}"

echo "==> eNurse deploy (BT Panel variant)"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: $ENV_FILE not found."
  echo "  cp $DEPLOY_DIR/.env.prod.example $ENV_FILE"
  echo "  then fill in secrets (JWT, SMTP, ADMIN_PASSWORD_HASH, domain)."
  exit 1
fi

# Required secrets sanity check (fail fast, before any build)
for key in ACCESS_TOKEN_SECRET REFRESH_TOKEN_SECRET ADMIN_PASSWORD_HASH; do
  val=$(grep -E "^${key}=" "$ENV_FILE" | head -1 | cut -d= -f2- || true)
  if [ -z "$val" ] || [ "$val" = "CHANGE_ME" ]; then
    echo "ERROR: $key is not set in $ENV_FILE"
    exit 1
  fi
done

echo "==> [1/5] Building web-admin (VITE_API_BASE=/api/v1, same-origin)"
cd web-admin
npm ci
VITE_API_BASE=/api/v1 npm run build
cd ..

echo "==> [2/5] Deploying web-admin to BT site root: $WEB_ROOT"
if [ ! -d "$WEB_ROOT" ]; then
  echo "ERROR: $WEB_ROOT not found."
  echo "  Create the site in BT Panel first (网站 → 添加站点, 纯静态),"
  echo "  or set WEB_ROOT=... to the site's root directory."
  exit 1
fi
rm -rf "$WEB_ROOT"/*
cp -r web-admin/dist/* "$WEB_ROOT/"
echo "  copied $(find "$WEB_ROOT" -type f | wc -l) files"

echo "==> [3/5] Validating compose file"
docker compose -f "$DEPLOY_DIR/docker-compose.prod.yml" --env-file "$ENV_FILE" config -q

echo "==> [4/5] Starting stack (postgres + redis + server)"
docker compose -f "$DEPLOY_DIR/docker-compose.prod.yml" --env-file "$ENV_FILE" up -d --build

echo "==> [5/5] Health check"
ok=0
for i in $(seq 1 20); do
  sleep 5
  # API is alive when it answers 401 (auth required) on the loopback port BT nginx proxies to
  api_code=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:3000/api/v1/admin/stats || true)
  if [ "$api_code" = "401" ]; then
    ok=1
    echo "OK: API answers 401 (auth required, expected)"
    break
  fi
  echo "  waiting... api=$api_code"
done

if [ "$ok" != "1" ]; then
  echo "ERROR: API health check failed. Logs:"
  docker compose -f "$DEPLOY_DIR/docker-compose.prod.yml" logs --tail 50 server
  exit 1
fi

# Web is served by BT nginx — warn only, the site config may not be set up yet
web_code=$(curl -ks -o /dev/null -w '%{http_code}' https://127.0.0.1/ || true)
if [ "$web_code" = "200" ]; then
  echo "OK: web answers 200 via BT nginx"
else
  echo "WARN: web check via https://127.0.0.1/ returned $web_code (expected 200)."
  echo "  Make sure the BT site exists, bt-site.conf is pasted, and SSL is applied."
fi

echo "==> Done. Useful commands:"
echo "  docker compose -f $DEPLOY_DIR/docker-compose.prod.yml logs -f server"
echo "  docker compose -f $DEPLOY_DIR/docker-compose.prod.yml ps"
echo "  (backup: see docs/deploy.md — daily pg_dump cron)"
