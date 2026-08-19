import client from './client';
import type { PageResponse, SalaryResponse, SalarySettingRequest, SalarySettingResponse } from '../types/api';

export async function fetchSalarySetting(): Promise<SalarySettingResponse> {
  const { data } = await client.get<SalarySettingResponse>('/api/salary-settings');
  return data;
}

export async function updateSalarySetting(request: SalarySettingRequest): Promise<SalarySettingResponse> {
  const { data } = await client.put<SalarySettingResponse>('/api/salary-settings', request);
  return data;
}

export async function calculateSalaries(yearMonth: string): Promise<SalaryResponse[]> {
  const { data } = await client.post<SalaryResponse[]>('/api/salaries/calculate', null, {
    params: { yearMonth },
  });
  return data;
}

export async function fetchSalaries(
  yearMonth: string,
  page: number,
  size: number,
): Promise<PageResponse<SalaryResponse>> {
  const { data } = await client.get<PageResponse<SalaryResponse>>('/api/salaries', {
    params: { yearMonth, page, size },
  });
  return data;
}
