export type UserRole = 'OWNER' | 'STAFF';
export type PartnerType = 'SUPPLIER' | 'CUSTOMER' | 'BOTH';
export type StockChangeType = 'PURCHASE_IN' | 'SALE_OUT' | 'ADJUST';
export type PurchaseStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELED';
export type SaleStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELED';
export type PaymentType = 'RECEIPT' | 'DISBURSEMENT';
export type PaymentStatus = 'CONFIRMED' | 'CANCELED';
export type LedgerType = 'RECEIVABLE' | 'PAYABLE';
export type LedgerChangeType =
  | 'SALE_CONFIRMED'
  | 'SALE_CANCELED'
  | 'PURCHASE_CONFIRMED'
  | 'PURCHASE_CANCELED'
  | 'PAYMENT_RECEIVED'
  | 'PAYMENT_RECEIVED_CANCELED'
  | 'PAYMENT_PAID'
  | 'PAYMENT_PAID_CANCELED';

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
  receivableBalance: number;
  payableBalance: number;
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

export interface ItemImageResponse {
  id: number;
  uploadFileName: string;
  fileType: string;
  fileSize: number;
  displayOrder: number;
  primary: boolean;
  createdAt: string;
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
  /**
   * 단건 조회는 전체 사진, 목록 조회는 카드에 쓸 대표 사진 1장만 담긴다
   * (목록에서 품목마다 전체 사진을 싣는 건 낭비라 백엔드가 그렇게 내려준다).
   * 사진이 없으면 빈 배열.
   */
  images: ItemImageResponse[];
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

export interface CompanyInfoRequest {
  companyName: string;
  businessNumber?: string;
  ceoName?: string;
  address?: string;
  phone?: string;
}

export interface CompanyInfoResponse {
  id: number;
  companyName: string;
  businessNumber: string | null;
  ceoName: string | null;
  address: string | null;
  phone: string | null;
  createdAt: string;
}

export interface PurchaseItemRequest {
  itemSpecId: number;
  quantity: number;
  unitPrice: number;
}

export interface PurchaseItemResponse {
  id: number;
  itemSpecId: number;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface PurchaseRequest {
  partnerId: number;
  companyInfoId: number;
  purchaseDate: string;
  memo?: string;
  items: PurchaseItemRequest[];
}

export interface PurchaseResponse {
  id: number;
  purchaseNo: string;
  partnerId: number;
  companyInfoId: number;
  purchaseDate: string;
  totalAmount: number;
  status: PurchaseStatus;
  memo: string | null;
  createdBy: number;
  createdAt: string;
  canceledAt: string | null;
  items: PurchaseItemResponse[];
}

export interface SaleItemRequest {
  itemSpecId: number;
  quantity: number;
  unitPrice: number;
}

export interface SaleItemResponse {
  id: number;
  itemSpecId: number;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface SaleRequest {
  partnerId: number;
  companyInfoId: number;
  saleDate: string;
  memo?: string;
  items: SaleItemRequest[];
}

export interface SaleResponse {
  id: number;
  saleNo: string;
  partnerId: number;
  companyInfoId: number;
  saleDate: string;
  totalAmount: number;
  status: SaleStatus;
  memo: string | null;
  createdBy: number;
  createdAt: string;
  canceledAt: string | null;
  items: SaleItemResponse[];
}

export interface PaymentRequest {
  partnerId: number;
  paymentType: PaymentType;
  amount: number;
  paymentDate: string;
  method?: string;
  memo?: string;
}

export interface PaymentResponse {
  id: number;
  paymentNo: string;
  partnerId: number;
  paymentType: PaymentType;
  amount: number;
  paymentDate: string;
  method: string | null;
  memo: string | null;
  status: PaymentStatus;
  createdBy: number;
  createdAt: string;
  canceledAt: string | null;
}

export interface LedgerEntryResponse {
  id: number;
  partnerId: number;
  ledgerType: LedgerType;
  changeType: LedgerChangeType;
  amount: number;
  balanceAfter: number;
  relatedDocumentType: string | null;
  relatedDocumentId: number | null;
  createdBy: number;
  createdAt: string;
}

export type LeaveType = 'FULL_DAY' | 'HALF_DAY';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface DepartmentRequest {
  name: string;
}

export interface DepartmentResponse {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
}

export interface PositionRequest {
  name: string;
  allowance: number;
}

export interface PositionResponse {
  id: number;
  name: string;
  allowance: number;
  active: boolean;
}

export interface EmployeeRequest {
  departmentId: number;
  positionId: number;
  companyUserId?: number;
  name: string;
  phone?: string;
  email?: string;
  hireDate: string;
}

export interface EmployeeResignRequest {
  resignationDate: string;
}

export interface EmployeeResponse {
  id: number;
  departmentId: number;
  positionId: number;
  companyUserId: number | null;
  name: string;
  phone: string | null;
  email: string | null;
  hireDate: string;
  resignationDate: string | null;
  active: boolean;
}

export interface AttendanceResponse {
  id: number;
  employeeId: number;
  workDate: string;
  clockIn: string | null;
  clockOut: string | null;
}

export interface AttendanceSummaryResponse {
  employeeId: number;
  yearMonth: string;
  workDays: number;
  totalLateMinutes: number;
  totalEarlyLeaveMinutes: number;
  totalOvertimeMinutes: number;
}

export interface LeaveRequestApplyRequest {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface LeaveRequestResponse {
  id: number;
  employeeId: number;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  leaveDays: number;
  reason: string | null;
  status: LeaveStatus;
}

export interface SalarySettingRequest {
  dailyWage: number;
}

export interface SalarySettingResponse {
  id: number;
  dailyWage: number;
}

export interface SalaryResponse {
  id: number;
  employeeId: number;
  payYearMonth: string;
  workDays: number;
  basePay: number;
  positionAllowance: number;
  overtimePay: number;
  grossPay: number;
  incomeTax: number;
  residentTax: number;
  nationalPension: number;
  healthInsurance: number;
  employmentInsurance: number;
  totalDeduction: number;
  netPay: number;
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
