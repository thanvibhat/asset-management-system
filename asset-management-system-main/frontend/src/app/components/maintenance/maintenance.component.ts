import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of, finalize } from 'rxjs';
import { MaintenanceService } from '../../services/operations.service';
import { AssetService } from '../../services/asset.service';
import { BusinessService } from '../../services/business.service';
import { AuthService } from '../../services/auth.service';
import { MaintenanceRecord, Asset } from '../../models/models';

@Component({
  selector: 'app-maintenance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './maintenance.component.html',
  styleUrls: ['./maintenance.component.css']
})
export class MaintenanceComponent implements OnInit {
  records: MaintenanceRecord[] = [];
  allAssets: Asset[] = [];
  loading = true;
  totalPages = 0;
  currentPage = 0;
  totalElements = 0;

  showModal = false;
  editMode = false;
  saving = false;
  error = '';
  selectedRecord: MaintenanceRecord | null = null;

  types = ['PREVENTIVE', 'CORRECTIVE', 'INSPECTION'];
  statuses = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  // Consolidated Filters
  categories: any[] = [];
  vendors: any[] = [];
  filters = { status: '', categoryId: '', vendorId: '', search: '', type: '' };
  historyStatus = ''; // Default to all in unified view
  underWarranty = false;

  // Asset Autocomplete
  assetSearch$ = new Subject<string>();
  assetSuggestions: Asset[] = [];
  selectedAsset: Asset | null = null;
  loadingAssets = false;
  showAssetDropdown = false;

  form: any = this.emptyForm();

  constructor(
    public auth: AuthService,
    private maintenanceService: MaintenanceService,
    private assetService: AssetService,
    private businessService: BusinessService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadVendors();
    this.load();
    
    this.assetSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) return of({ content: [] });
        this.loadingAssets = true;
        return this.assetService.getAssets({ search: query }).pipe(
          finalize(() => this.loadingAssets = false)
        );
      })
    ).subscribe(res => {
      this.assetSuggestions = res.content;
      this.showAssetDropdown = this.assetSuggestions.length > 0;
    });
  }

  loadCategories(): void {
    this.assetService.getCategories().subscribe(c => this.categories = c);
  }

  loadVendors(): void {
    this.businessService.getVendorsList().subscribe(v => this.vendors = v);
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.load();
  }

  resetFilters(): void {
    this.filters = { status: '', categoryId: '', vendorId: '', search: '', type: '' };
    this.applyFilters();
  }

  load(): void {
    this.loading = true;
    // Map unified filters to record status
    const statusFilter = this.filters.status;
    this.maintenanceService.getAll(this.currentPage, 10, statusFilter).subscribe({
      next: p => { 
        this.records = p.content; 
        this.totalPages = p.totalPages; 
        this.totalElements = p.totalElements; 
        this.loading = false; 
      },
      error: () => { this.loading = false; }
    });
  }

  onHistoryStatusChange(): void {
    this.currentPage = 0;
    this.load();
  }

  openCreate(asset: Asset | null = null): void { 
    this.form = this.emptyForm(); 
    this.editMode = false; 
    this.error = ''; 
    this.showModal = true; 
    if (asset) {
      this.selectAsset(asset);
    } else {
      this.selectedAsset = null;
      this.underWarranty = false;
    }
  }

  openEdit(r: MaintenanceRecord): void {
    this.form = { ...r, assetId: r.assetId, maintenanceType: r.maintenanceType, status: r.status };
    this.selectedAsset = { id: r.assetId, assetTag: r.assetTag, name: r.assetName } as Asset;
    this.editMode = true; 
    this.error = ''; 
    this.showModal = true;
  }

  onAssetInput(event: any): void {
    const val = event.target.value;
    if (!val) {
      this.selectedAsset = null;
      this.form.assetId = null;
    }
    this.assetSearch$.next(val);
  }

  selectAsset(asset: Asset): void {
    this.selectedAsset = asset;
    this.form.assetId = asset.id;
    this.showAssetDropdown = false;
    this.checkWarranty(asset);
  }

  checkWarranty(asset: Asset): void {
    if (asset.purchaseDate && asset.warrantyMonths) {
      const pDate = new Date(asset.purchaseDate);
      const expiryDate = new Date(pDate.setMonth(pDate.getMonth() + asset.warrantyMonths));
      this.underWarranty = expiryDate > new Date();
    } else {
      this.underWarranty = false;
    }
  }
  closeModal(): void { this.showModal = false; }

  save(): void {
    if (!this.form.assetId || !this.form.maintenanceType || !this.form.description) {
      this.error = 'Asset, type and description are required.'; return;
    }
    this.saving = true; this.error = '';
    const obs = this.editMode
      ? this.maintenanceService.update(this.form.id, this.form)
      : this.maintenanceService.create(this.form);

    obs.subscribe({
      next: () => { 
        this.saving = false; 
        this.closeModal(); 
        this.load(); 
      },
      error: err => { this.error = err.error?.message || 'Error saving record'; this.saving = false; }
    });
  }

  completeRecord(r: MaintenanceRecord): void {
    const updated = { ...r, status: 'COMPLETED', completedDate: new Date().toISOString().split('T')[0] };
    this.maintenanceService.update(r.id, updated).subscribe(() => {
      this.load();
    });
  }

  cancelRecord(r: MaintenanceRecord): void {
    if (confirm('Are you sure you want to cancel this maintenance record?')) {
      const updated = { ...r, status: 'CANCELLED' };
      this.maintenanceService.update(r.id, updated).subscribe(() => {
        this.load();
      });
    }
  }

  showHistoryModal = false;
  historyAsset: any = null;
  assetHistory: any[] = [];
  loadingHistory = false;

  openHistory(assetOrId: any, assetTag?: string, assetName?: string): void {
    let assetId: number;
    if (typeof assetOrId === 'object' && assetOrId !== null) {
      assetId = assetOrId.id || assetOrId.assetId;
      this.historyAsset = {
        id: assetId,
        assetTag: assetOrId.assetTag,
        name: assetOrId.name || assetOrId.assetName
      };
    } else {
      assetId = Number(assetOrId);
      this.historyAsset = {
        id: assetId,
        assetTag: assetTag || '',
        name: assetName || ''
      };
    }
    
    this.assetHistory = [];
    this.showHistoryModal = true;
    this.loadingHistory = true;
    
    this.assetService.getAssetHistory(assetId).subscribe({
      next: (data) => {
        this.assetHistory = data;
        this.loadingHistory = false;
      },
      error: () => {
        this.loadingHistory = false;
      }
    });
  }

  closeHistoryModal(): void {
    this.showHistoryModal = false;
    this.historyAsset = null;
    this.assetHistory = [];
  }

  getEventIcon(eventType: string): string {
    const map: any = {
      PURCHASED: '🛒', ALLOCATED: '👤', RETURNED: '↩️', REASSIGNED: '🔁',
      MAINTENANCE_STARTED: '🔧', MAINTENANCE_COMPLETED: '✅',
      STATUS_CHANGED: '🔄', COMPONENT_REPLACED: '🔩',
      WARRANTY_UPDATED: '📋', RETIRED: '📦'
    };
    return map[eventType] || '•';
  }

  parseMeta(raw: any): { key: string; value: string }[] {
    if (!raw) return [];
    try {
      const obj = typeof raw === 'string' ? JSON.parse(raw) : raw;
      if (Object.keys(obj).length === 0) return [];
      return Object.entries(obj)
        .filter(([, v]) => v !== null && v !== undefined && v !== '')
        .map(([k, v]) => ({ key: k, value: String(v) }));
    } catch { return []; }
  }

  goToPage(p: number): void { this.currentPage = p; this.load(); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  badgeClass(status: string): string {
    return 'badge badge-' + status.toLowerCase().replace('_', '_');
  }

  typeBadge(type: string): string {
    const m: any = { PREVENTIVE: 'badge-active', CORRECTIVE: 'badge-lost', INSPECTION: 'badge-allocated' };
    return 'badge ' + (m[type] || '');
  }

  private emptyForm() {
    return { assetId: null, maintenanceType: 'PREVENTIVE', description: '', cost: null, performedBy: '', scheduledDate: '', completedDate: '', status: 'SCHEDULED' };
  }

  formatStatus(status: string): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase().split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  }
}
