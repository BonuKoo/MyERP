import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { clockIn, clockOut, fetchMonthlySummary } from '../api/attendance';

export function useMonthlySummary(employeeId: number | undefined, yearMonth: string) {
  return useQuery({
    queryKey: ['attendanceSummary', employeeId, yearMonth],
    queryFn: () => fetchMonthlySummary(employeeId as number, yearMonth),
    enabled: employeeId !== undefined,
  });
}

/** 출퇴근은 "오늘 내 기록"만 다루므로 별도 무효화 키 없이 요약 쿼리를 통째로 무효화한다. */
export function useAttendanceMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['attendanceSummary'] });

  const clockInMutation = useMutation({
    mutationFn: clockIn,
    onSuccess: invalidate,
  });

  const clockOutMutation = useMutation({
    mutationFn: clockOut,
    onSuccess: invalidate,
  });

  return { clockInMutation, clockOutMutation };
}
