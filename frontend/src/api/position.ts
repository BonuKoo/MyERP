import client from './client';
import type { PageResponse, PositionRequest, PositionResponse } from '../types/api';

export async function fetchPositions(page: number, size: number): Promise<PageResponse<PositionResponse>> {
  const { data } = await client.get<PageResponse<PositionResponse>>('/api/positions', {
    params: { page, size },
  });
  return data;
}

export async function createPosition(request: PositionRequest): Promise<PositionResponse> {
  const { data } = await client.post<PositionResponse>('/api/positions', request);
  return data;
}

export async function updatePosition(id: number, request: PositionRequest): Promise<PositionResponse> {
  const { data } = await client.put<PositionResponse>(`/api/positions/${id}`, request);
  return data;
}

export async function deactivatePosition(id: number): Promise<void> {
  await client.delete(`/api/positions/${id}`);
}
