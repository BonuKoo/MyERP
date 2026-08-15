import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createPartner, deactivatePartner, fetchPartners, updatePartner } from '../api/partner';
import type { PartnerRequest } from '../types/api';

/** 거래처 목록 쿼리 무효화에 쓰는 공용 키 */
export const PARTNERS_QUERY_KEY = ['partners'] as const;

/** 거래처 페이징 목록 조회. */
export function usePartnerList(page: number, size: number) {
  return useQuery({
    queryKey: [...PARTNERS_QUERY_KEY, page, size],
    queryFn: () => fetchPartners(page, size),
  });
}

/**
 * 거래처 등록/수정/비활성화 뮤테이션을 모은 훅.
 * 목록 쿼리를 직접 구독하지 않아, 폼 페이지에서 써도 불필요한 목록 조회가 일어나지 않는다.
 */
export function usePartnerMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: PARTNERS_QUERY_KEY });

  const createMutation = useMutation({
    mutationFn: (request: PartnerRequest) => createPartner(request),
    onSuccess: invalidate,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: PartnerRequest }) => updatePartner(id, request),
    onSuccess: invalidate,
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => deactivatePartner(id),
    onSuccess: invalidate,
  });

  return { createMutation, updateMutation, deactivateMutation };
}
