import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createDepartment,
  deactivateDepartment,
  fetchDepartments,
  updateDepartment,
} from '../api/department';
import type { DepartmentRequest } from '../types/api';

export const DEPARTMENTS_QUERY_KEY = ['departments'] as const;

export function useDepartmentList(page = 0, size = 100) {
  return useQuery({
    queryKey: [...DEPARTMENTS_QUERY_KEY, page, size],
    queryFn: () => fetchDepartments(page, size),
  });
}

export function useDepartmentMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: DEPARTMENTS_QUERY_KEY });

  const createMutation = useMutation({
    mutationFn: (request: DepartmentRequest) => createDepartment(request),
    onSuccess: invalidate,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: DepartmentRequest }) => updateDepartment(id, request),
    onSuccess: invalidate,
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => deactivateDepartment(id),
    onSuccess: invalidate,
  });

  return { createMutation, updateMutation, deactivateMutation };
}
