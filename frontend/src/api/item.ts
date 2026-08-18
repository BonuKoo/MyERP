import client, { API_BASE_URL } from './client';
import type {
  ItemImageResponse,
  ItemRequest,
  ItemResponse,
  ItemSpecRequest,
  ItemSpecResponse,
  PageResponse,
} from '../types/api';

export async function fetchItems(
  page: number,
  size: number,
  categoryMainId?: number | null,
  categorySubId?: number | null,
): Promise<PageResponse<ItemResponse>> {
  const { data } = await client.get<PageResponse<ItemResponse>>('/api/items', {
    params: {
      page,
      size,
      categoryMainId: categoryMainId ?? undefined,
      categorySubId: categorySubId ?? undefined,
    },
  });
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

export async function updateItem(id: number, request: ItemRequest): Promise<ItemResponse> {
  const { data } = await client.put<ItemResponse>(`/api/items/${id}`, request);
  return data;
}

/**
 * 이미지 URL은 axios가 아니라 <img src>가 직접 부르므로 절대 URL 문자열을 만들어준다.
 * 이 경로만 백엔드에서 인증 없이 열려 있다 — img 태그는 Authorization 헤더를 붙일 수
 * 없기 때문이다(SecurityConfig 참고).
 */
export function itemImageUrl(itemId: number, imageId: number, size?: 'thumb'): string {
  const query = size ? `?size=${size}` : '';
  return `${API_BASE_URL}/api/items/${itemId}/images/${imageId}${query}`;
}

export async function fetchItemImages(itemId: number): Promise<ItemImageResponse[]> {
  const { data } = await client.get<ItemImageResponse[]>(`/api/items/${itemId}/images`);
  return data;
}

export async function uploadItemImages(itemId: number, files: File[]): Promise<ItemImageResponse[]> {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));

  const { data } = await client.post<ItemImageResponse[]>(`/api/items/${itemId}/images`, formData, {
    // client는 기본 Content-Type이 application/json이다. FormData를 보낼 땐 반드시
    // 지워야 브라우저가 multipart boundary가 포함된 헤더를 직접 채운다 —
    // 남겨두면 서버가 파트를 파싱하지 못한다.
    headers: { 'Content-Type': undefined },
  });
  return data;
}

export async function deleteItemImage(itemId: number, imageId: number): Promise<void> {
  await client.delete(`/api/items/${itemId}/images/${imageId}`);
}

export async function setPrimaryItemImage(itemId: number, imageId: number): Promise<void> {
  await client.patch(`/api/items/${itemId}/images/${imageId}/primary`);
}

export async function fetchItemSpecs(itemId: number): Promise<ItemSpecResponse[]> {
  const { data } = await client.get<ItemSpecResponse[]>(`/api/items/${itemId}/specs`);
  return data;
}

export async function createItemSpec(itemId: number, request: ItemSpecRequest): Promise<ItemSpecResponse> {
  const { data } = await client.post<ItemSpecResponse>(`/api/items/${itemId}/specs`, request);
  return data;
}
