import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cancelPayment, createPayment, fetchPayments } from '../api/payment';
import type { PaymentRequest } from '../types/api';
import { PARTNERS_QUERY_KEY } from './usePartners';
import { PARTNER_LEDGER_QUERY_KEY } from './usePartnerLedger';

export const PAYMENTS_QUERY_KEY = ['payments'] as const;

/** 결제 전체 목록(거래처 무관) 페이징 조회. */
export function usePaymentList(page: number, size: number) {
  return useQuery({
    queryKey: [...PAYMENTS_QUERY_KEY, page, size],
    queryFn: () => fetchPayments(page, size),
  });
}

/**
 * 결제 등록/취소는 거래처 잔액(receivableBalance/payableBalance)과 원장 이력을 함께
 * 바꾸므로, 성공 시 거래처 목록·상세와 원장 쿼리를 모두 무효화한다. 전체 결제
 * 목록(PAYMENTS_QUERY_KEY)도 같은 이유로 함께 무효화한다.
 */
export function usePaymentMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: PARTNERS_QUERY_KEY });
    queryClient.invalidateQueries({ queryKey: PARTNER_LEDGER_QUERY_KEY });
    queryClient.invalidateQueries({ queryKey: PAYMENTS_QUERY_KEY });
  };

  const createMutation = useMutation({
    mutationFn: (request: PaymentRequest) => createPayment(request),
    onSuccess: invalidate,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => cancelPayment(id),
    onSuccess: invalidate,
  });

  return { createMutation, cancelMutation };
}
