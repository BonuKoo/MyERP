import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createEmployee,
  fetchEmployee,
  fetchEmployees,
  fetchMyEmployee,
  resignEmployee,
  updateEmployee,
} from '../api/employee';
import type { EmployeeFilters } from '../api/employee';
import type { EmployeeRequest, EmployeeResignRequest } from '../types/api';

export const EMPLOYEES_QUERY_KEY = ['employees'] as const;
export const MY_EMPLOYEE_QUERY_KEY = ['employees', 'me'] as const;

export function useEmployeeList(page: number, size: number, filters: EmployeeFilters) {
  return useQuery({
    queryKey: [...EMPLOYEES_QUERY_KEY, page, size, filters],
    queryFn: () => fetchEmployees(page, size, filters),
  });
}

export function useEmployeeDetail(id: number) {
  return useQuery({
    queryKey: [...EMPLOYEES_QUERY_KEY, id],
    queryFn: () => fetchEmployee(id),
  });
}

/**
 * 로그인 계정에 연결된 내 사원 정보. 연결이 없는 계정(예: 사원 마스터에
 * 등록하지 않은 OWNER)이 정상적으로 존재하므로 404를 재시도하거나 에러로
 * 취급하지 않는다 — 호출부가 `query.data === undefined`로 "미연결"을 판단한다.
 */
export function useMyEmployee() {
  return useQuery({
    queryKey: MY_EMPLOYEE_QUERY_KEY,
    queryFn: fetchMyEmployee,
    retry: false,
  });
}

export function useEmployeeMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: EMPLOYEES_QUERY_KEY });

  const createMutation = useMutation({
    mutationFn: (request: EmployeeRequest) => createEmployee(request),
    onSuccess: invalidate,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: EmployeeRequest }) => updateEmployee(id, request),
    onSuccess: invalidate,
  });

  const resignMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: EmployeeResignRequest }) => resignEmployee(id, request),
    onSuccess: invalidate,
  });

  return { createMutation, updateMutation, resignMutation };
}
