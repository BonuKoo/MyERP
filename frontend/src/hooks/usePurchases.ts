import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cancelPurchase, createPurchase, fetchPurchase, fetchPurchases } from '../api/purchase';
import type { PurchaseRequest } from '../types/api';
import { ITEM_SPECS_QUERY_KEY } from './useItems';

export const PURCHASES_QUERY_KEY = ['purchases'] as const;

export function usePurchaseList(page: number, size: number) {
  return useQuery({
    queryKey: [...PURCHASES_QUERY_KEY, page, size],
    queryFn: () => fetchPurchases(page, size),
  });
}

export function usePurchaseDetail(id: number | null) {
  return useQuery({
    queryKey: [...PURCHASES_QUERY_KEY, id],
    queryFn: () => fetchPurchase(id as number),
    enabled: id !== null,
  });
}

/** 매입 등록/취소는 규격의 재고를 변경하므로 성공 시 매입 목록과 규격 목록을 함께 무효화한다. */
export function usePurchaseMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: PURCHASES_QUERY_KEY });
    queryClient.invalidateQueries({ queryKey: ITEM_SPECS_QUERY_KEY });
  };

  const createMutation = useMutation({
    mutationFn: (request: PurchaseRequest) => createPurchase(request),
    onSuccess: invalidate,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => cancelPurchase(id),
    onSuccess: invalidate,
  });

  return { createMutation, cancelMutation };
}
