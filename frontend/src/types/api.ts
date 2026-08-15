export type UserRole = 'OWNER' | 'STAFF';
export type PartnerType = 'SUPPLIER' | 'CUSTOMER' | 'BOTH';
export type StockChangeType = 'PURCHASE_IN' | 'SALE_OUT' | 'ADJUST';

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

export interface CategoryMainRequest {
  name: string;
  displayOrder: number;
}

export interface CategoryMainResponse {
  id: number;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface CategorySubRequest {
  categoryMainId: number;
  name: string;
  displayOrder: number;
}

export interface CategorySubResponse {
  id: number;
  categoryMainId: number;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface CertificationRequest {
  name: string;
}

export interface CertificationResponse {
  id: number;
  name: string;
}

export interface ItemRequest {
  categorySubId: number;
  name: string;
  description?: string;
  ksStandard?: string;
  certificationIds?: number[];
}

export interface ItemResponse {
  id: number;
  categorySubId: number;
  name: string;
  description: string | null;
  ksStandard: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  certifications: CertificationResponse[];
}

export interface ItemSpecRequest {
  specName: string;
  unit: string;
  costPrice: number;
  salePrice: number;
  safetyStock: number;
}

export interface ItemSpecResponse {
  id: number;
  itemId: number;
  specName: string;
  unit: string;
  costPrice: number;
  salePrice: number;
  currentStock: number;
  safetyStock: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface StockAdjustRequest {
  quantityDelta: number;
}

export interface StockHistoryResponse {
  id: number;
  itemSpecId: number;
  changeType: StockChangeType;
  quantity: number;
  beforeStock: number;
  afterStock: number;
  relatedDocumentType: string | null;
  relatedDocumentId: number | null;
  createdBy: number;
  createdAt: string;
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
