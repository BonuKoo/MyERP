export type UserRole = 'OWNER' | 'STAFF';
export type PartnerType = 'SUPPLIER' | 'CUSTOMER' | 'BOTH';

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  email: string;
  name: string;
  role: UserRole;
}

export interface CompanyUserResponse {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export interface PartnerRequest {
  name: string;
  businessNumber?: string;
  partnerType: PartnerType;
  contactName?: string;
  contactPhone?: string;
  address?: string;
}

export interface PartnerResponse {
  id: number;
  name: string;
  businessNumber: string | null;
  partnerType: PartnerType;
  contactName: string | null;
  contactPhone: string | null;
  address: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalCount: number;
  page: number;
  size: number;
}

export interface ErrorResponse {
  status: number;
  message: string;
  timestamp: string;
}
