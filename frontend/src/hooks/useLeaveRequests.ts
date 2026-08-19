import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  applyLeave,
  approveLeaveRequest,
  fetchLeaveRequestsByEmployee,
  rejectLeaveRequest,
} from '../api/leaveRequest';
import type { LeaveRequestApplyRequest } from '../types/api';

function leaveRequestsQueryKey(employeeId: number) {
  return ['leaveRequests', employeeId] as const;
}

export function useLeaveRequestsByEmployee(employeeId: number | undefined) {
  return useQuery({
    queryKey: leaveRequestsQueryKey(employeeId as number),
    queryFn: () => fetchLeaveRequestsByEmployee(employeeId as number),
    enabled: employeeId !== undefined,
  });
}

export function useLeaveRequestMutations(employeeId: number | undefined) {
  const queryClient = useQueryClient();
  const invalidate = () => {
    if (employeeId !== undefined) {
      queryClient.invalidateQueries({ queryKey: leaveRequestsQueryKey(employeeId) });
    }
  };

  const applyMutation = useMutation({
    mutationFn: (request: LeaveRequestApplyRequest) => applyLeave(employeeId as number, request),
    onSuccess: invalidate,
  });

  const approveMutation = useMutation({
    mutationFn: (id: number) => approveLeaveRequest(id),
    onSuccess: invalidate,
  });

  const rejectMutation = useMutation({
    mutationFn: (id: number) => rejectLeaveRequest(id),
    onSuccess: invalidate,
  });

  return { applyMutation, approveMutation, rejectMutation };
}
