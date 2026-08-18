import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createItem,
  createItemSpec,
  deleteItemImage,
  fetchItem,
  fetchItemSpecs,
  fetchItems,
  setPrimaryItemImage,
  updateItem,
  uploadItemImages,
} from '../api/item';
import type { ItemRequest, ItemSpecRequest } from '../types/api';

export const ITEMS_QUERY_KEY = ['items'] as const;
export const ITEM_SPECS_QUERY_KEY = ['itemSpecs'] as const;

export function useItemList(
  page: number,
  size: number,
  categoryMainId?: number | null,
  categorySubId?: number | null,
) {
  return useQuery({
    queryKey: [...ITEMS_QUERY_KEY, page, size, categoryMainId ?? null, categorySubId ?? null],
    queryFn: () => fetchItems(page, size, categoryMainId, categorySubId),
  });
}

export function useItemDetail(id: number | null) {
  return useQuery({
    queryKey: [...ITEMS_QUERY_KEY, id],
    queryFn: () => fetchItem(id as number),
    enabled: id !== null,
  });
}

export function useItemSpecs(itemId: number | null) {
  return useQuery({
    queryKey: [...ITEM_SPECS_QUERY_KEY, itemId],
    queryFn: () => fetchItemSpecs(itemId as number),
    enabled: itemId !== null,
  });
}

export function useItemMutations() {
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (request: ItemRequest) => createItem(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ITEMS_QUERY_KEY }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: ItemRequest }) => updateItem(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ITEMS_QUERY_KEY }),
  });

  return { createMutation, updateMutation };
}

/**
 * 사진을 바꾸면 상세(전체 사진)와 목록(대표 사진 썸네일)이 모두 달라지므로
 * items 쿼리 전체를 무효화한다.
 */
export function useItemImageMutations(itemId: number) {
  const queryClient = useQueryClient();

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ITEMS_QUERY_KEY });

  const uploadMutation = useMutation({
    mutationFn: (files: File[]) => uploadItemImages(itemId, files),
    onSuccess: invalidate,
  });

  const deleteMutation = useMutation({
    mutationFn: (imageId: number) => deleteItemImage(itemId, imageId),
    onSuccess: invalidate,
  });

  const setPrimaryMutation = useMutation({
    mutationFn: (imageId: number) => setPrimaryItemImage(itemId, imageId),
    onSuccess: invalidate,
  });

  return { uploadMutation, deleteMutation, setPrimaryMutation };
}

export function useItemSpecMutations(itemId: number) {
  const queryClient = useQueryClient();

  const createSpecMutation = useMutation({
    mutationFn: (request: ItemSpecRequest) => createItemSpec(itemId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...ITEM_SPECS_QUERY_KEY, itemId] }),
  });

  return { createSpecMutation };
}
