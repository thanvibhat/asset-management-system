import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AllocationService } from '../../services/operations.service';
import { AssetService } from '../../services/asset.service';
import { AuthService } from '../../services/auth.service';
import { AllocationFormComponent } from './allocation-form/allocation-form.component';
import { Allocation, Asset, User, AssetCategory } from '../../models/models';

@Component({
  selector: 'app-allocation',
  standalone: true,
  imports: [CommonModule, FormsModule, AllocationFormComponent],
  templateUrl: './allocation.component.html',
  styleUrls: ['./allocation.component.css']
})
export class AllocationComponent implements OnInit {
  assets: Asset[] = [];
  categories: AssetCategory[] = [];
  loading = true;
  
  totalPages = 0;
  currentPage = 0;
  totalElements = 0;

  selectedAssetForModal: Asset | null = null;
  
  // Consolidated Filters
  filters: any = { search: '', status: '', categoryId: '' };

  showModal = false;
  saving = false;
  error = '';
  returnConfirmId: number | null = null;

  constructor(
    public auth: AuthService,
    private allocationService: AllocationService,
    private assetService: AssetService
  ) {}

  ngOnInit(): void {
    this.loadAssets();
    this.loadCategories();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadAssets();
  }

  resetFilters(): void {
    this.filters = { search: '', status: '', categoryId: '' };
    this.applyFilters();
  }

  loadAssets(): void {
    this.loading = true;
    this.assetService.getAssets({
      page: this.currentPage,
      size: 10,
      search: this.filters.search,
      categoryId: this.filters.categoryId,
      status: this.filters.status
    }).subscribe({
      next: (p: any) => {
        this.assets = p.content;
        this.totalPages = p.totalPages;
        this.totalElements = p.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  loadCategories(): void {
    this.assetService.getCategories().subscribe(c => this.categories = c);
  }

  openModal(asset: Asset | null = null): void {
    this.selectedAssetForModal = asset;
    this.error = '';
    this.showModal = true;
  }

  closeModal(): void { this.showModal = false; }

  save(payload: any): void {
    this.saving = true; this.error = '';
    this.allocationService.allocate(payload).subscribe({
      next: () => {
        this.saving = false;
        this.closeModal();
        this.loadAssets();
      },
      error: err => { this.error = err.error?.message || 'Error allocating asset'; this.saving = false; }
    });
  }

  confirmReturn(id: number): void { this.returnConfirmId = id; }
  cancelReturn(): void { this.returnConfirmId = null; }
  returnAsset(): void {
    if (!this.returnConfirmId) return;
    this.allocationService.returnAsset(this.returnConfirmId).subscribe({
      next: () => { this.returnConfirmId = null; this.loadAssets(); },
      error: err => alert(err.error?.message || 'Error returning asset')
    });
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

  goToPage(p: number): void { this.currentPage = p; this.loadAssets(); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  badgeClass(status: string): string {
    return 'badge badge-' + status.toLowerCase();
  }

  formatStatus(status: string): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase().split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  }
}
