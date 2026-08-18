import { useQuery } from '@tanstack/react-query';
import { fetchPartnerLedger } from '../api/ledger';

export const PARTNER_LEDGER_QUERY_KEY = ['partnerLedger'] as const;

export function usePartnerLedger(partnerId: number, page: number, size: number) {
  return useQuery({
    queryKey: [...PARTNER_LEDGER_QUERY_KEY, partnerId, page, size],
    queryFn: () => fetchPartnerLedger(partnerId, page, size),
  });
}
