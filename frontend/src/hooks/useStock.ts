import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adjustStock, fetchStockHistory } from '../api/stock';
import type { StockAdjustRequest } from '../types/api';
import { ITEM_SPECS_QUERY_KEY } from './useItems';

export const STOCK_HISTORY_QUERY_KEY = ['stockHistory'] as const;

export function useStockHistory(itemSpecId: number | null, page: number, size: number) {
  return useQuery({
    queryKey: [...STOCK_HISTORY_QUERY_KEY, itemSpecId, page, size],
    queryFn: () => fetchStockHistory(itemSpecId as number, page, size),
    enabled: itemSpecId !== null,
  });
}

/** 특정 품목(itemId)에 속한 규격의 재고를 조정하는 뮤테이션. 성공 시 규격 목록과 이력을 무효화한다. */
export function useStockAdjustMutation(itemId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ itemSpecId, request }: { itemSpecId: number; request: StockAdjustRequest }) =>
      adjustStock(itemSpecId, request),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [...ITEM_SPECS_QUERY_KEY, itemId] });
      queryClient.invalidateQueries({ queryKey: [...STOCK_HISTORY_QUERY_KEY, variables.itemSpecId] });
    },
  });
}
