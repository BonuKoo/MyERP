import client from './client';
import type { ItemSpecResponse, PageResponse, StockAdjustRequest, StockHistoryResponse } from '../types/api';

export async function adjustStock(itemSpecId: number, request: StockAdjustRequest): Promise<ItemSpecResponse> {
  const { data } = await client.post<ItemSpecResponse>(`/api/item-specs/${itemSpecId}/stock/adjust`, request);
  return data;
}

export async function fetchStockHistory(
  itemSpecId: number,
  page: number,
  size: number,
): Promise<PageResponse<StockHistoryResponse>> {
  const { data } = await client.get<PageResponse<StockHistoryResponse>>(
    `/api/item-specs/${itemSpecId}/stock/history`,
    { params: { page, size } },
  );
  return data;
}
