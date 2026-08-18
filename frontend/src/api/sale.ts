import client from './client';
import type { PageResponse, SaleRequest, SaleResponse } from '../types/api';

export async function fetchSales(page: number, size: number): Promise<PageResponse<SaleResponse>> {
  const { data } = await client.get<PageResponse<SaleResponse>>('/api/sales', { params: { page, size } });
  return data;
}

export async function fetchSale(id: number): Promise<SaleResponse> {
  const { data } = await client.get<SaleResponse>(`/api/sales/${id}`);
  return data;
}

export async function createSale(request: SaleRequest): Promise<SaleResponse> {
  const { data } = await client.post<SaleResponse>('/api/sales', request);
  return data;
}

export async function cancelSale(id: number): Promise<SaleResponse> {
  const { data } = await client.post<SaleResponse>(`/api/sales/${id}/cancel`);
  return data;
}
