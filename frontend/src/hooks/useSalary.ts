import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { calculateSalaries, fetchSalarySetting, fetchSalaries, updateSalarySetting } from '../api/salary';
import type { SalarySettingRequest } from '../types/api';

export const SALARY_SETTING_QUERY_KEY = ['salarySetting'] as const;

export function useSalarySetting() {
  return useQuery({
    queryKey: SALARY_SETTING_QUERY_KEY,
    queryFn: fetchSalarySetting,
  });
}

export function useSalarySettingMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: SalarySettingRequest) => updateSalarySetting(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SALARY_SETTING_QUERY_KEY }),
  });
}

export function useSalaryList(yearMonth: string, page: number, size: number) {
  return useQuery({
    queryKey: ['salaries', yearMonth, page, size],
    queryFn: () => fetchSalaries(yearMonth, page, size),
    enabled: yearMonth !== '',
  });
}

export function useCalculateSalaries() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (yearMonth: string) => calculateSalaries(yearMonth),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['salaries'] }),
  });
}
