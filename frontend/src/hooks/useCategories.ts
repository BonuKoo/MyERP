import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createCategoryMain, createCategorySub, fetchCategoryMains, fetchCategorySubs } from '../api/category';
import type { CategoryMainRequest, CategorySubRequest } from '../types/api';

export const CATEGORY_MAIN_QUERY_KEY = ['categoryMains'] as const;
export const CATEGORY_SUB_QUERY_KEY = ['categorySubs'] as const;

export function useCategoryMains() {
  return useQuery({
    queryKey: CATEGORY_MAIN_QUERY_KEY,
    queryFn: fetchCategoryMains,
  });
}

export function useCategorySubs(mainId: number | null) {
  return useQuery({
    queryKey: [...CATEGORY_SUB_QUERY_KEY, mainId],
    queryFn: () => fetchCategorySubs(mainId as number),
    enabled: mainId !== null,
  });
}

export function useCategoryMutations() {
  const queryClient = useQueryClient();

  const createMainMutation = useMutation({
    mutationFn: (request: CategoryMainRequest) => createCategoryMain(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CATEGORY_MAIN_QUERY_KEY }),
  });

  const createSubMutation = useMutation({
    mutationFn: (request: CategorySubRequest) => createCategorySub(request),
    onSuccess: (_, variables) =>
      queryClient.invalidateQueries({ queryKey: [...CATEGORY_SUB_QUERY_KEY, variables.categoryMainId] }),
  });

  return { createMainMutation, createSubMutation };
}
