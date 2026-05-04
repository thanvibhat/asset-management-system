export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
  enabled: boolean;
  department?: string;
  employeeId?: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
  permissions: string[];
}

export interface Notification {
  id: number;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface DashboardStats {
  totalAssets: number;
  allocatedAssets: number;
  availableAssets: number;
  maintenanceAssets: number;
  totalValue: number;
  categoryDistribution: { [key: string]: number };
  statusDistribution: { [key: string]: number };
}

export interface Asset {
  id: number;
  productId?: number;
  assetTag: string;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  status: AssetStatus;
  purchaseDate: string;
  purchaseCost: number;
  currentValue: number;
  location: string;
  serialNumber: string;
  manufacturer: string;
  model: string;
  warrantyMonths: number;
  procurementId?: number;
  dynamicAttributes?: any; // JSON string or object
  createdAt: string;
  currentAllocationId?: number;
  assignedToFullName?: string;
  assignedToUsername?: string;
  allocatedAt?: string;
  expectedReturnDate?: string;
  warrantyExpiryDate?: string;
}

export type AssetStatus = 'AVAILABLE' | 'ALLOCATED' | 'UNDER_MAINTENANCE' | 'DAMAGED' | 'LOST' | 'RETIRED';

export interface AssetCategory {
  id: number;
  name: string;
  description: string;
  attributeSchema?: any; // Array of field definitions
}

export interface Allocation {
  id: number;
  assetId: number;
  assetTag: string;
  assetName: string;
  userId: number;
  userName: string;
  userFullName: string;
  allocatedByName: string;
  allocatedAt: string;
  returnedAt: string | null;
  expectedReturnDate: string;
  notes: string;
  status: 'ACTIVE' | 'RETURNED' | 'OVERDUE';
}

export interface MaintenanceRecord {
  id: number;
  assetId: number;
  assetTag: string;
  assetName: string;
  maintenanceType: 'PREVENTIVE' | 'CORRECTIVE' | 'INSPECTION';
  description: string;
  cost: number;
  performedBy: string;
  scheduledDate: string;
  completedDate: string | null;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
}


export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface MaintenanceAnalytics {
  totalCost: number;
  totalRecords: number;
  costByCategory: { [key: string]: number };
  countByType: { [key: string]: number };
  monthlyTrends: MonthlyTrend[];
  topCostlyAssets: AssetCostItem[];
}

export interface ProcurementAnalytics {
  totalSpend: number;
  totalQuantity: number;
  spendByVendor: { [key: string]: number };
  monthlySpendTrends: MonthlyTrend[];
}

export interface MonthlyTrend {
  month: string;
  value: number;
  count: number;
}

export interface AssetCostItem {
  assetId: number;
  assetTag: string;
  assetName: string;
  totalCost: number;
  maintenanceCount: number;
}

export interface AssetMetricsDto {
  assetId: number;
  assetTag: string;
  assetName: string;
  category: string;
  totalMaintenanceCost: number;
  correctiveRepairCount: number;
  preventiveCount: number;
  maintenanceToPurchaseRatio: number;
  averageDaysBetweenRepairs: number;
  currentValueRetentionPct: number;
  ageInDays: number;
}
