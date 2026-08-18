import client from './client';
import type { CertificationRequest, CertificationResponse } from '../types/api';

export async function fetchCertifications(): Promise<CertificationResponse[]> {
  const { data } = await client.get<CertificationResponse[]>('/api/certifications');
  return data;
}

export async function createCertification(request: CertificationRequest): Promise<CertificationResponse> {
  const { data } = await client.post<CertificationResponse>('/api/certifications', request);
  return data;
}
