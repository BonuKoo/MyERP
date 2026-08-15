import client from './client';
import type {
  ItemRequest,
  ItemResponse,
  ItemSpecRequest,
  ItemSpecResponse,
  PageResponse,
} from '../types/api';

export async function fetchItems(page: number, size: number): Promise<PageResponse<ItemResponse>> {
  const { data } = await client.get<PageResponse<ItemResponse>>('/api/items', { params: { page, size } });
  return data;
}

export async function fetchItem(id: number): Promise<ItemResponse> {
  const { data } = await client.get<ItemResponse>(`/api/items/${id}`);
  return data;
}

export async function createItem(request: ItemRequest): Promise<ItemResponse> {
  const { data } = await client.post<ItemResponse>('/api/items', request);
  return data;
}

export async function fetchItemSpecs(itemId: number): Promise<ItemSpecResponse[]> {
  const { data } = await client.get<ItemSpecResponse[]>(`/api/items/${itemId}/specs`);
  return data;
}

export async function createItemSpec(itemId: number, request: ItemSpecRequest): Promise<ItemSpecResponse> {
  const { data } = await client.post<ItemSpecResponse>(`/api/items/${itemId}/specs`, request);
  return data;
}
