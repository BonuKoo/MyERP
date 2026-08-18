import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createCategoryMain,
  createCategorySub,
  fetchAllCategorySubs,
  fetchCategoryMains,
  fetchCategorySubs,
} from '../api/category';
import type { CategoryMainRequest, CategorySubRequest } from '../types/api';

export const CATEGORY_MAIN_QUERY_KEY = ['categoryMains'] as const;
export const CATEGORY_SUB_QUERY_KEY = ['categorySubs'] as const;
export const ALL_CATEGORY_SUBS_QUERY_KEY = ['allCategorySubs'] as const;

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

/** 대분류 구분 없이 전체 중분류. 품목 목록에서 categorySubId → 이름 표시용. */
export function useAllCategorySubs() {
  return useQuery({
    queryKey: ALL_CATEGORY_SUBS_QUERY_KEY,
    queryFn: fetchAllCategorySubs,
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
