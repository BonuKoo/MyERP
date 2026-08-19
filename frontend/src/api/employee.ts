import client from './client';
import type { EmployeeRequest, EmployeeResignRequest, EmployeeResponse, PageResponse } from '../types/api';

export interface EmployeeFilters {
  departmentId?: number;
  positionId?: number;
  name?: string;
}

export async function fetchEmployees(
  page: number,
  size: number,
  filters: EmployeeFilters,
): Promise<PageResponse<EmployeeResponse>> {
  const { data } = await client.get<PageResponse<EmployeeResponse>>('/api/employees', {
    params: { page, size, ...filters },
  });
  return data;
}

export async function fetchEmployee(id: number): Promise<EmployeeResponse> {
  const { data } = await client.get<EmployeeResponse>(`/api/employees/${id}`);
  return data;
}

/** 로그인 계정에 연결된 내 사원 정보. 연결이 없으면 404(호출부에서 처리). */
export async function fetchMyEmployee(): Promise<EmployeeResponse> {
  const { data } = await client.get<EmployeeResponse>('/api/employees/me');
  return data;
}

export async function createEmployee(request: EmployeeRequest): Promise<EmployeeResponse> {
  const { data } = await client.post<EmployeeResponse>('/api/employees', request);
  return data;
}

export async function updateEmployee(id: number, request: EmployeeRequest): Promise<EmployeeResponse> {
  const { data } = await client.put<EmployeeResponse>(`/api/employees/${id}`, request);
  return data;
}

export async function resignEmployee(id: number, request: EmployeeResignRequest): Promise<void> {
  await client.patch(`/api/employees/${id}/resign`, request);
}
