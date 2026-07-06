import { PrismaClient } from '../generated/prisma/client';
import { PrismaPg } from '@prisma/adapter-pg';
import * as bcrypt from 'bcrypt';
import 'dotenv/config';

async function main() {
  const adapter = new PrismaPg({
    connectionString: process.env.DATABASE_URL,
  });
  const prisma = new PrismaClient({ adapter });

  // Create admin account
  const adminPassword = await bcrypt.hash(
    process.env.ADMIN_PASSWORD || 'admin_enurse_2026',
    12,
  );
  await prisma.user.upsert({
    where: { email: 'admin@enurse.com' },
    update: {},
    create: {
      username: 'admin',
      email: 'admin@enurse.com',
      passwordHash: adminPassword,
      role: 'ADMIN',
      displayName: '系统管理员',
    },
  });

  console.log('Seed complete: admin account created.');
  await prisma.$disconnect();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
