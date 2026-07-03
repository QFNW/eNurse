/**
 * Module verification — checks NestJS dependency injection wiring.
 * Run: npx tsx test/verify-modules.ts
 */
import { Test } from '@nestjs/testing';
import { AppModule } from '../src/app.module';
import { PrismaService } from '../src/prisma/prisma.service';

async function main() {
  const mockPrisma = {
    $connect: () => Promise.resolve(),
    $disconnect: () => Promise.resolve(),
  };

  const module = await Test.createTestingModule({
    imports: [AppModule],
  })
    .overrideProvider(PrismaService)
    .useValue(mockPrisma as any)
    .compile();

  const controllers = [
    'AuthController', 'UsersController', 'BmController',
    'MedicationsController', 'AlertsController', 'GroupsController',
    'AdminController',
  ];

  console.log('=== eNurse Controller Verification ===\n');
  let passed = 0;

  for (const name of controllers) {
    try {
      module.get(name);
      console.log(`  ✅ ${name}`);
      passed++;
    } catch {
      console.log(`  ❌ ${name}`);
    }
  }

  console.log(`\n=== ${passed}/${controllers.length} controllers loaded ===`);
  if (passed < controllers.length) process.exit(1);
}

main().catch((e) => {
  console.error('Failed:', e.message?.slice(0, 300));
  process.exit(1);
});
