import client from './client';
import type { CompanyUserResponse, LoginRequest, LoginResponse, RegisterRequest } from '../types/api';

export async function register(request: RegisterRequest): Promise<CompanyUserResponse> {
  const { data } = await client.post<CompanyUserResponse>('/api/auth/register', request);
  return data;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await client.post<LoginResponse>('/api/auth/login', request);
  return data;
}
