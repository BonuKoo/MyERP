import client from './client';
import type { CategoryMainRequest, CategoryMainResponse, CategorySubRequest, CategorySubResponse } from '../types/api';

export async function fetchCategoryMains(): Promise<CategoryMainResponse[]> {
  const { data } = await client.get<CategoryMainResponse[]>('/api/categories/main');
  return data;
}

export async function createCategoryMain(request: CategoryMainRequest): Promise<CategoryMainResponse> {
  const { data } = await client.post<CategoryMainResponse>('/api/categories/main', request);
  return data;
}

export async function fetchCategorySubs(mainId: number): Promise<CategorySubResponse[]> {
  const { data } = await client.get<CategorySubResponse[]>(`/api/categories/main/${mainId}/sub`);
  return data;
}

export async function createCategorySub(request: CategorySubRequest): Promise<CategorySubResponse> {
  const { data } = await client.post<CategorySubResponse>('/api/categories/sub', request);
  return data;
}
