import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AssetService } from '../../services/asset.service';
import { MasterService } from '../../services/master.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Asset } from '../../models/models';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transfer.component.html',
  styleUrls: ['./transfer.component.css']
})
export class TransferComponent implements OnInit, OnDestroy {
  unallocatedAssets: Asset[] = [];
  filteredAssets: Asset[] = [];
  loading = true;
  saving = false;
  searchQuery = '';

  // Transfer Modal State
  showTransferModal = false;
  selectedAsset: Asset | null = null;
  newLocation = '';
  showLocationDropdown = false;
  locationSuggestions: string[] = [];
  allLocations: string[] = [];

  // History Modal State
  showHistoryModal = false;
  historyAsset: any = null;
  assetHistory: any[] = [];
  loadingHistory = false;

  private destroy$ = new Subject<void>();

  constructor(
    private assetService: AssetService,
    private masterService: MasterService,
    public auth: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadAssets();
    this.loadLocations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAssets(): void {
    this.loading = true;
    // Fetch a large page to get most/all assets for client-side filtering and robust search
    this.assetService.getAssets({ page: 0, size: 1000 }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (res) => {
        // Filter out allocated assets (only unallocated assets are allowed)
        this.unallocatedAssets = (res.content || []).filter(a => a.status !== 'ALLOCATED');
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load assets:', err);
        this.toastService.error('Failed to load assets');
        this.loading = false;
      }
    });
  }

  loadLocations(): void {
    this.masterService.getLocationsList().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (res) => {
        this.allLocations = (res || []).map(l => typeof l === 'string' ? l : l.name);
      },
      error: (err) => {
        console.error('Failed to load locations:', err);
      }
    });
  }

  applyFilter(): void {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) {
      this.filteredAssets = [...this.unallocatedAssets];
      return;
    }
    this.filteredAssets = this.unallocatedAssets.filter(a => 
      a.name.toLowerCase().includes(q) || 
      a.assetTag.toLowerCase().includes(q) ||
      (a.serialNumber && a.serialNumber.toLowerCase().includes(q)) ||
      (a.location && a.location.toLowerCase().includes(q))
    );
  }

  openTransferModal(asset: Asset): void {
    this.selectedAsset = asset;
    this.newLocation = '';
    this.locationSuggestions = [];
    this.showLocationDropdown = false;
    this.showTransferModal = true;
  }

  closeTransferModal(): void {
    this.showTransferModal = false;
    this.selectedAsset = null;
    this.newLocation = '';
  }

  filterLocations(event: any): void {
    const val = event.target.value;
    this.newLocation = val;
    if (!val.trim()) {
      this.locationSuggestions = [];
      this.showLocationDropdown = false;
      return;
    }
    this.locationSuggestions = this.allLocations
      .filter(loc => loc.toLowerCase().includes(val.toLowerCase()) && loc.toLowerCase() !== val.toLowerCase())
      .slice(0, 6);
    this.showLocationDropdown = this.locationSuggestions.length > 0;
  }

  selectLocation(loc: string): void {
    this.newLocation = loc;
    this.showLocationDropdown = false;
  }

  executeTransfer(): void {
    if (!this.selectedAsset || !this.newLocation.trim()) return;
    this.saving = true;

    this.assetService.transferAsset(this.selectedAsset.id, this.newLocation.trim()).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (updatedAsset) => {
        this.toastService.success(`Successfully transferred ${updatedAsset.assetTag} to ${updatedAsset.location}!`);
        this.closeTransferModal();
        this.loadAssets(); // Reload list to update local state
        this.loadLocations(); // Refresh locations list
        this.saving = false;
      },
      error: (err) => {
        console.error('Transfer failed:', err);
        this.toastService.error(err.error?.message || 'Transfer failed');
        this.saving = false;
      }
    });
  }

  // History Modal Logic
  openHistory(asset: any): void {
    this.historyAsset = asset;
    this.showHistoryModal = true;
    this.loadingHistory = true;
    this.assetHistory = [];
    
    this.assetService.getAssetHistory(asset.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (history) => {
        this.assetHistory = history || [];
        this.loadingHistory = false;
      },
      error: (err) => {
        console.error('History failed:', err);
        this.toastService.error('Failed to load asset history');
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
      WARRANTY_UPDATED: '📋', RETIRED: '📦', TRANSFERRED: '🚚'
    };
    return map[eventType] || '•';
  }

  parseMeta(meta: any): string {
    if (!meta) return '';
    try {
      const parsed = typeof meta === 'string' ? JSON.parse(meta) : meta;
      return Object.entries(parsed)
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ');
    } catch {
      return '';
    }
  }

  badgeClass(status: string): string {
    const map: any = {
      AVAILABLE: 'badge-available',
      ALLOCATED: 'badge-allocated',
      UNDER_MAINTENANCE: 'badge-maintenance',
      DAMAGED: 'badge-damaged',
      RETIRED: 'badge-retired',
      LOST: 'badge-lost'
    };
    return 'badge ' + (map[status] || 'badge-neutral');
  }

  formatStatus(status: string): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase().split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  }
}
