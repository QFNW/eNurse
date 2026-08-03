// Patch Prisma v7 generated client for CJS runtime:
// 1. Replace ESM-only import.meta.url with CJS __dirname
// 2. Rewrite ".ts" module specifiers to ".js" — Prisma 7.8+ emits
//    `import ... from "./internal/class.ts"` which tsc keeps as-is in
//    CommonJS output, and Node cannot require(".ts") files.
const fs = require('fs');
const path = require('path');

const generatedDir = path.join(__dirname, 'dist', 'generated', 'prisma');
if (!fs.existsSync(generatedDir)) {
  console.error('Prisma client not found at', generatedDir);
  process.exit(1);
}

function patchFile(file) {
  let content = fs.readFileSync(file, 'utf8');
  const original = content;
  const dir = path.dirname(file).replace(/\\/g, '/');
  content = content.replace(/import\.meta\.url/g, `'file:///${dir}'`);
  content = content.replace(/(require\(|import\()["']([^"']+)\.ts["']\)/g, '$1"$2.js")');
  if (content !== original) {
    fs.writeFileSync(file, content);
    console.log('patched:', path.relative(__dirname, file));
  }
}

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(p);
    else if (entry.name.endsWith('.js')) patchFile(p);
  }
}

walk(generatedDir);
console.log('Prisma client patched successfully');
