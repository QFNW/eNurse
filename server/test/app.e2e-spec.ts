import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from '../src/app.module';

describe('Auth (e2e)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
      }),
    );
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('/api/v1/auth/register (POST) — validate empty body', () => {
    return request(app.getHttpServer())
      .post('/api/v1/auth/register')
      .send({})
      .expect(400);
  });

  it('/api/v1/auth/register (POST) — validate invalid email', () => {
    return request(app.getHttpServer())
      .post('/api/v1/auth/register')
      .send({ email: 'not-email', password: '12345678', role: 'PATIENT' })
      .expect(400);
  });

  it('/api/v1/auth/register (POST) — validate short password', () => {
    return request(app.getHttpServer())
      .post('/api/v1/auth/register')
      .send({ email: 'test@test.com', password: '123', role: 'PATIENT' })
      .expect(400);
  });

  it('/api/v1/auth/login (POST) — validate missing fields', () => {
    return request(app.getHttpServer())
      .post('/api/v1/auth/login')
      .send({})
      .expect(400);
  });
});
