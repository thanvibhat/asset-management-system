export interface Vendor {
  id?: number;
  name: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  contactPerson?: string;
  vendorType?: 'PROCUREMENT' | 'MAINTENANCE' | 'BOTH';
  gstNumber?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Procurement {
  id?: number;
  poNumber: string;
  vendor?: Vendor;
  orderDate?: string;
  totalCost?: number;
  status?: string;
  invoiceNumber?: string;
  quantity?: number;
  vendorId?: number;
  createdBy?: any;
  createdAt?: string;
  updatedAt?: string;
}

export interface DashboardStats {
  totalAssets: number;
  availableAssets: number;
  allocatedAssets: number;
  maintenanceAssets: number;
  retiredAssets: number;
  totalAssetValue: number;
  activeUsers: number;
}
