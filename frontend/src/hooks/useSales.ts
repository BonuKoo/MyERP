import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cancelSale, createSale, fetchSale, fetchSales } from '../api/sale';
import type { SaleRequest } from '../types/api';
import { ITEM_SPECS_QUERY_KEY } from './useItems';

export const SALES_QUERY_KEY = ['sales'] as const;

export function useSaleList(page: number, size: number) {
  return useQuery({
    queryKey: [...SALES_QUERY_KEY, page, size],
    queryFn: () => fetchSales(page, size),
  });
}

export function useSaleDetail(id: number | null) {
  return useQuery({
    queryKey: [...SALES_QUERY_KEY, id],
    queryFn: () => fetchSale(id as number),
    enabled: id !== null,
  });
}

/** 매출 등록/취소는 규격의 재고를 변경하므로 성공 시 매출 목록과 규격 목록을 함께 무효화한다. */
export function useSaleMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: SALES_QUERY_KEY });
    queryClient.invalidateQueries({ queryKey: ITEM_SPECS_QUERY_KEY });
  };

  const createMutation = useMutation({
    mutationFn: (request: SaleRequest) => createSale(request),
    onSuccess: invalidate,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => cancelSale(id),
    onSuccess: invalidate,
  });

  return { createMutation, cancelMutation };
}
