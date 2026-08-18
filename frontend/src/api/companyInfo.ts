import client from './client';
import type { CompanyInfoRequest, CompanyInfoResponse } from '../types/api';

export async function fetchCompanyInfoList(): Promise<CompanyInfoResponse[]> {
  const { data } = await client.get<CompanyInfoResponse[]>('/api/company-info');
  return data;
}

export async function createCompanyInfo(request: CompanyInfoRequest): Promise<CompanyInfoResponse> {
  const { data } = await client.post<CompanyInfoResponse>('/api/company-info', request);
  return data;
}
