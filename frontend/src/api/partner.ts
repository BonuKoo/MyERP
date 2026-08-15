import client from './client';
import type { PageResponse, PartnerRequest, PartnerResponse } from '../types/api';

export async function fetchPartners(page: number, size: number): Promise<PageResponse<PartnerResponse>> {
  const { data } = await client.get<PageResponse<PartnerResponse>>('/api/partners', {
    params: { page, size },
  });
  return data;
}

export async function fetchPartner(id: number): Promise<PartnerResponse> {
  const { data } = await client.get<PartnerResponse>(`/api/partners/${id}`);
  return data;
}

export async function createPartner(request: PartnerRequest): Promise<PartnerResponse> {
  const { data } = await client.post<PartnerResponse>('/api/partners', request);
  return data;
}

export async function updatePartner(id: number, request: PartnerRequest): Promise<PartnerResponse> {
  const { data } = await client.put<PartnerResponse>(`/api/partners/${id}`, request);
  return data;
}

export async function deactivatePartner(id: number): Promise<void> {
  await client.delete(`/api/partners/${id}`);
}
