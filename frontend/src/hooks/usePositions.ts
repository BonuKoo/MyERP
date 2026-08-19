import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createPosition, deactivatePosition, fetchPositions, updatePosition } from '../api/position';
import type { PositionRequest } from '../types/api';

export const POSITIONS_QUERY_KEY = ['positions'] as const;

export function usePositionList(page = 0, size = 100) {
  return useQuery({
    queryKey: [...POSITIONS_QUERY_KEY, page, size],
    queryFn: () => fetchPositions(page, size),
  });
}

export function usePositionMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: POSITIONS_QUERY_KEY });

  const createMutation = useMutation({
    mutationFn: (request: PositionRequest) => createPosition(request),
    onSuccess: invalidate,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: PositionRequest }) => updatePosition(id, request),
    onSuccess: invalidate,
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => deactivatePosition(id),
    onSuccess: invalidate,
  });

  return { createMutation, updateMutation, deactivateMutation };
}
