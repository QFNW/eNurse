// Patch Prisma v7 generated client: replace ESM-only import.meta.url with CJS __dirname
const fs = require('fs');
const path = require('path');

const clientPath = path.join(__dirname, 'dist', 'generated', 'prisma', 'client.js');
if (!fs.existsSync(clientPath)) {
    console.error('Prisma client not found at', clientPath);
    process.exit(1);
}

let content = fs.readFileSync(clientPath, 'utf8');
const dirPath = path.dirname(clientPath).replace(/\\/g, '/');

// Replace import.meta.url with hardcoded directory path
content = content.replace(
    /import\.meta\.url/g,
    `'file:///${dirPath}'`,
);

fs.writeFileSync(clientPath, content);
console.log('Prisma client patched successfully');
