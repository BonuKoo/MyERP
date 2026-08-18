import client from './client';
import type { PageResponse, PaymentRequest, PaymentResponse } from '../types/api';

export async function fetchPayments(page: number, size: number): Promise<PageResponse<PaymentResponse>> {
  const { data } = await client.get<PageResponse<PaymentResponse>>('/api/payments', { params: { page, size } });
  return data;
}

export async function fetchPayment(id: number): Promise<PaymentResponse> {
  const { data } = await client.get<PaymentResponse>(`/api/payments/${id}`);
  return data;
}

export async function createPayment(request: PaymentRequest): Promise<PaymentResponse> {
  const { data } = await client.post<PaymentResponse>('/api/payments', request);
  return data;
}

export async function cancelPayment(id: number): Promise<PaymentResponse> {
  const { data } = await client.post<PaymentResponse>(`/api/payments/${id}/cancel`);
  return data;
}
