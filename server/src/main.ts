import { NestFactory } from '@nestjs/core';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.enableCors({
    origin: process.env.CORS_ORIGIN || 'http://localhost:5173',
    credentials: true,
  });

  // Swagger / OpenAPI documentation
  const config = new DocumentBuilder()
    .setTitle('eNurse API')
    .setDescription('易护 — 患者生理体征追踪护理系统')
    .setVersion('1.0')
    .addBearerAuth()
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document);

  const port = process.env.PORT ?? 3000;
  await app.listen(port);
  console.log(`eNurse server running on http://localhost:${port}`);
  console.log(`API Docs: http://localhost:${port}/api/docs`);
}
bootstrap();
