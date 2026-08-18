import client from './client';
import type { LedgerEntryResponse, PageResponse } from '../types/api';

export async function fetchPartnerLedger(
  partnerId: number,
  page: number,
  size: number,
): Promise<PageResponse<LedgerEntryResponse>> {
  const { data } = await client.get<PageResponse<LedgerEntryResponse>>(`/api/partners/${partnerId}/ledger`, {
    params: { page, size },
  });
  return data;
}
