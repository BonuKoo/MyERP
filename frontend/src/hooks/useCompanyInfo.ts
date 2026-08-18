import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createCompanyInfo, fetchCompanyInfoList } from '../api/companyInfo';
import type { CompanyInfoRequest } from '../types/api';

export const COMPANY_INFO_QUERY_KEY = ['companyInfo'] as const;

export function useCompanyInfoList() {
  return useQuery({
    queryKey: COMPANY_INFO_QUERY_KEY,
    queryFn: fetchCompanyInfoList,
  });
}

export function useCompanyInfoMutations() {
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (request: CompanyInfoRequest) => createCompanyInfo(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: COMPANY_INFO_QUERY_KEY }),
  });

  return { createMutation };
}
