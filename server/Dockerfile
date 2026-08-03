# eNurse server — production image
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
# Prisma schema + config first so generate works without app sources
COPY prisma ./prisma
COPY prisma.config.ts ./
# generate only parses schema/config; placeholder URL, real one comes at runtime via env_file
ENV DATABASE_URL=postgresql://placeholder:placeholder@localhost:5432/placeholder
RUN npx prisma generate
COPY . .
# nest build && node patch-prisma.js (fixes Prisma v7 ESM/CJS path)
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/generated ./generated
COPY --from=build /app/prisma ./prisma
COPY --from=build /app/prisma.config.ts ./
COPY --from=build /app/package.json ./
# Run migrations, then start (entry is dist/src/main.js — start:prod script is stale)
CMD ["sh", "-c", "npx prisma migrate deploy && node dist/src/main.js"]
