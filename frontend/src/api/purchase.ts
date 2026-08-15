import client from './client';
import type { PageResponse, PurchaseRequest, PurchaseResponse } from '../types/api';

export async function fetchPurchases(page: number, size: number): Promise<PageResponse<PurchaseResponse>> {
  const { data } = await client.get<PageResponse<PurchaseResponse>>('/api/purchases', { params: { page, size } });
  return data;
}

export async function fetchPurchase(id: number): Promise<PurchaseResponse> {
  const { data } = await client.get<PurchaseResponse>(`/api/purchases/${id}`);
  return data;
}

export async function createPurchase(request: PurchaseRequest): Promise<PurchaseResponse> {
  const { data } = await client.post<PurchaseResponse>('/api/purchases', request);
  return data;
}

export async function cancelPurchase(id: number): Promise<PurchaseResponse> {
  const { data } = await client.post<PurchaseResponse>(`/api/purchases/${id}/cancel`);
  return data;
}
