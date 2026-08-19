import client from './client';
import type { DepartmentRequest, DepartmentResponse, PageResponse } from '../types/api';

export async function fetchDepartments(page: number, size: number): Promise<PageResponse<DepartmentResponse>> {
  const { data } = await client.get<PageResponse<DepartmentResponse>>('/api/departments', {
    params: { page, size },
  });
  return data;
}

export async function createDepartment(request: DepartmentRequest): Promise<DepartmentResponse> {
  const { data } = await client.post<DepartmentResponse>('/api/departments', request);
  return data;
}

export async function updateDepartment(id: number, request: DepartmentRequest): Promise<DepartmentResponse> {
  const { data } = await client.put<DepartmentResponse>(`/api/departments/${id}`, request);
  return data;
}

export async function deactivateDepartment(id: number): Promise<void> {
  await client.delete(`/api/departments/${id}`);
}
