import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createItem, createItemSpec, fetchItem, fetchItemSpecs, fetchItems } from '../api/item';
import type { ItemRequest, ItemSpecRequest } from '../types/api';

export const ITEMS_QUERY_KEY = ['items'] as const;
export const ITEM_SPECS_QUERY_KEY = ['itemSpecs'] as const;

export function useItemList(page: number, size: number) {
  return useQuery({
    queryKey: [...ITEMS_QUERY_KEY, page, size],
    queryFn: () => fetchItems(page, size),
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

  return { createMutation };
}

export function useItemSpecMutations(itemId: number) {
  const queryClient = useQueryClient();

  const createSpecMutation = useMutation({
    mutationFn: (request: ItemSpecRequest) => createItemSpec(itemId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...ITEM_SPECS_QUERY_KEY, itemId] }),
  });

  return { createSpecMutation };
}
