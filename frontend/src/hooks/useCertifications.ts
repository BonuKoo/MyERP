import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createCertification, fetchCertifications } from '../api/certification';
import type { CertificationRequest } from '../types/api';

export const CERTIFICATIONS_QUERY_KEY = ['certifications'] as const;

export function useCertificationList() {
  return useQuery({
    queryKey: CERTIFICATIONS_QUERY_KEY,
    queryFn: fetchCertifications,
  });
}

export function useCertificationMutations() {
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (request: CertificationRequest) => createCertification(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CERTIFICATIONS_QUERY_KEY }),
  });

  return { createMutation };
}
