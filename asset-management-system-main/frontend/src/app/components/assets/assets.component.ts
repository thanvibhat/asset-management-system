import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AssetService } from '../../services/asset.service';
import { MasterService } from '../../services/master.service';
import { AuthService } from '../../services/auth.service';
import { BusinessService } from '../../services/business.service';
import { Asset, AssetCategory } from '../../models/models';
import { Vendor } from '../../models/business.model';
import { AssetFormComponent } from './asset-form/asset-form.component';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-assets',
  standalone: true,
  imports: [CommonModule, FormsModule, AssetFormComponent],
  templateUrl: './assets.component.html',
  styleUrls: ['./assets.component.css']
})
export class AssetsComponent implements OnInit {
  assets: Asset[] = [];
  categories: AssetCategory[] = [];
  loading = true;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 10;
  today = new Date().toISOString().split('T')[0];
  vendors: Vendor[] = [];
  loadingMore = false;

  // New properties for product-first flow
  products: any[] = [];
  locations: any[] = [];
  manufacturers: any[] = [];
  locationSuggestions: string[] = [];
  manufacturerSuggestions: string[] = [];
  showLocationDropdown = false;
  showManufacturerDropdown = false;
  loadingNextTag = false;
  selectedAsset: any = null;

  // Import properties
  showImportModal = false;
  importFile: File | null = null;
  importing = false;
  importResult: { imported: number; failed: number; errors: string[] } | null = null;

  filters = { status: '', categoryId: '', search: '', vendorId: '' };
  showModal = false;
  editMode = false;
  saving = false;
  error = '';
  deleteConfirmId: number | null = null;

  form: any = null;
  statuses = ['AVAILABLE','ALLOCATED','UNDER_MAINTENANCE','DAMAGED','RETIRED','LOST'];
  
  // Existing Component management properties
  assetComponents: any[] = [];
  loadingComponents = false;
  showReplaceForm = false;

  // Three-dot menu
  openMenuId: number | null = null;

  // History modal
  showHistoryModal = false;
  historyAsset: any = null;
  assetHistory: any[] = [];
  loadingHistory = false;

  // Warranty modal
  showWarrantyModal = false;
  showLinkingModal = false;
  warrantyAsset: any = null;
  linkingAsset: any = null;
  linkedDevices: any[] = [];
  
  // Manual linking state
  showAddDeviceSearch = false;
  deviceSearchQuery = '';
  deviceSearchResults: any[] = [];

  replaceRequest = {
    componentType: '',
    serialNumber: '',
    warrantyMonths: undefined as number | undefined
  };

  // New Component replacement modal properties
  showReplaceModal = false;
  replacingAsset: any = null;
  replaceForm = {
    componentType: '',
    serialNumber: '',
    warrantyMonths: null as number | null,
    oldComponentDisposition: ''
  };
  replaceSaving = false;
  componentTypes = ['Battery','RAM','HDD','SSD','Display','Keyboard',
                    'Charger','Mouse','Motherboard','Other'];
  dispositionOptions = [
    { value: 'DISCARDED',          label: 'Discarded' },
    { value: 'UNDER_MAINTENANCE',  label: 'Under Maintenance' },
    { value: 'RETURNED_TO_VENDOR', label: 'Returned to Vendor' },
    { value: 'REPURPOSED_INTERNAL',label: 'Repurposed Internally' },
    { value: 'IN_STORAGE',         label: 'Stored' },
    { value: 'DONATED',            label: 'Donated' }
  ];
  
  private searchSubject = new Subject<string>();

  constructor(
    public auth: AuthService,
    private assetService: AssetService,
    private businessService: BusinessService,
    private masterService: MasterService,
    private toast: ToastService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadVendors();
    this.loadAssets();
    this.masterService.getProductsList().subscribe(p => this.products = p);
    this.masterService.getLocationsList().subscribe(l => this.locations = l.map((x: any) => x.name));
    this.masterService.getManufacturersList().subscribe(m => this.manufacturers = m.map((x: any) => x.name));

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.applyFilters();
    });
  }

  onSearchChange(): void {
    this.searchSubject.next(this.filters.search);
  }

  // Removed redundant handlers, now in AssetFormComponent

  loadAssets(): void {
    if (this.currentPage === 0) {
      this.loading = true;
    } else {
      this.loadingMore = true;
    }

    const searchVal = this.filters.search?.trim();
    const filters: any = { page: this.currentPage, size: this.pageSize };
    if (this.filters.status) filters.status = this.filters.status;
    if (this.filters.categoryId) filters.categoryId = +this.filters.categoryId;
    if (searchVal) filters.search = searchVal;
    
    this.assetService.getAssets(filters).subscribe({
      next: p => {
        const newAssets = p?.content || [];
        
        this.assets = newAssets;

        this.totalElements = p?.totalElements || 0;
        this.totalPages = p?.totalPages || 0;
        this.loading = false;
        this.loadingMore = false;
      },
      error: () => { 
        this.loading = false; 
        this.loadingMore = false;
        this.assets = this.currentPage === 0 ? [] : this.assets;
      }
    });
  }



  loadVendors(): void {
    this.businessService.getVendorsList().subscribe(v => this.vendors = v);
  }

  loadCategories(): void {
    this.assetService.getCategories().subscribe(c => this.categories = c);
  }

  applyFilters(): void { this.currentPage = 0; this.loadAssets(); }
  resetFilters(): void { this.filters = { status: '', categoryId: '', search: '', vendorId: '' }; this.applyFilters(); }

  openAddAsset(): void { 
    this.selectedAsset = null;
    this.editMode = false; 
    this.showModal = true; 
    this.error = ''; 
  }

  openEdit(a: Asset): void {
    this.selectedAsset = { ...a };
    
    // Handle dynamic attributes
    if (typeof this.selectedAsset.dynamicAttributes === 'string') {
      try {
        this.selectedAsset.dynamicAttributes = JSON.parse(this.selectedAsset.dynamicAttributes);
      } catch (e) {
        this.selectedAsset.dynamicAttributes = {};
      }
    }
    
    this.editMode = true; 
    this.showModal = true; 
    this.error = '';
    this.loadComponents(a.id);
  }

  openExcelUpload(): void { this.showImportModal = true; this.importFile = null; this.importResult = null; }
  closeImportModal(): void { this.showImportModal = false; }

  onFileSelected(event: any): void {
    this.importFile = event.target.files[0] || null;
  }

  downloadTemplate(): void {
    this.assetService.downloadExcelTemplate().subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a'); 
      a.href = url; 
      a.download = 'asset_import_template.xlsx';
      a.click(); 
      window.URL.revokeObjectURL(url);
    });
  }

  downloadAllAssetsExcel(): void {
    this.assetService.exportAssets().subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `assets_export_${new Date().toISOString().split('T')[0]}.xlsx`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Failed to export assets')
    });
  }

  importAssets(): void {
    if (!this.importFile) return;
    this.importing = true;
    const formData = new FormData();
    formData.append('file', this.importFile);
    this.http.post<any>('/api/assets/upload', formData).subscribe({
      next: result => { 
        this.importResult = result; 
        this.importing = false; 
        this.loadAssets();
        if (result.imported > 0) {
          this.toast.success(`${result.imported} assets uploaded successfully`);
        }
        if (result.failed > 0) {
          this.toast.error(`${result.failed} rows failed to upload`);
        }
      },
      error: err => { 
        this.importing = false; 
        this.toast.error(err.error?.message || 'Import failed');
      }
    });
  }

  closeModal(): void { this.showModal = false; }

  save(payload: any): void {
    this.saving = true; this.error = '';
    
    const isNew = !this.editMode;
    const obs = this.editMode
      ? this.assetService.updateAsset(payload.id, payload)
      : this.assetService.createAsset(payload);
      
    obs.subscribe({
      next: () => { 
        this.saving = false; 
        this.closeModal(); 
        
        if (isNew) {
          this.currentPage = 0; // Reset to first page to show new asset at top
          this.toast.success('Asset created successfully');
        } else {
          this.toast.success('Asset updated successfully');
        }
        
        this.loadAssets(); 
      },
      error: err => { 
        this.error = err.error?.message || 'Error saving asset'; 
        this.saving = false; 
        this.toast.error(this.error);
      }
    });
  }

  confirmDelete(id: number): void { this.deleteConfirmId = id; }
  cancelDelete(): void { this.deleteConfirmId = null; }
  deleteAsset(): void {
    if (!this.deleteConfirmId) return;
    this.assetService.deleteAsset(this.deleteConfirmId).subscribe({
      next: () => { this.deleteConfirmId = null; this.loadAssets(); },
      error: err => alert(err.error?.message || 'Error deleting asset')
    });
  }

  goToPage(page: number): void { this.currentPage = page; this.loadAssets(); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  loadComponents(assetId: number): void {
    this.loadingComponents = true;
    this.assetService.getComponents(assetId).subscribe({
      next: (data) => {
        this.assetComponents = data;
        this.loadingComponents = false;
      },
      error: () => { this.loadingComponents = false; }
    });
  }

  toggleMenu(assetId: number, event: MouseEvent): void {
    event.stopPropagation();
    this.openMenuId = this.openMenuId === assetId ? null : assetId;
  }

  closeMenus(): void { this.openMenuId = null; }

  openHistory(asset: any): void {
    this.historyAsset = asset;
    this.assetHistory = [];
    this.showHistoryModal = true;
    this.loadingHistory = true;
    this.openMenuId = null;
    this.assetService.getAssetHistory(asset.id).subscribe({
      next: (data) => { this.assetHistory = data; this.loadingHistory = false; },
      error: () => { this.loadingHistory = false; }
    });
  }

  closeHistoryModal(): void {
    this.showHistoryModal = false;
    this.historyAsset = null;
    this.assetHistory = [];
  }

  openWarrantyInfo(asset: any): void {
    this.warrantyAsset = asset;
    this.showWarrantyModal = true;
    this.openMenuId = null;
    this.loadComponents(asset.id);
  }

  closeWarrantyModal(): void {
    this.showWarrantyModal = false;
    this.warrantyAsset = null;
    this.assetComponents = [];
  }

  openDeviceLinking(asset: any): void {
    this.linkingAsset = asset;
    this.showLinkingModal = true;
    this.openMenuId = null;
    this.loadLinkingData(asset.id);
  }

  loadLinkingData(assetId: number): void {
    // Fetch child assets
    this.http.get<any[]>(`/api/assets/${assetId}/children`).subscribe({
      next: (assets) => {
        // Fetch components
        this.http.get<any[]>(`/api/assets/${assetId}/components`).subscribe({
          next: (components) => {
            // Combine them into a unified list
            const combined: any[] = [
              ...assets.map(a => ({ ...a, isAsset: true })),
              ...components.map(c => ({ ...c, isComponent: true }))
            ];
            this.linkedDevices = combined;
          },
          error: (err) => console.error('Error fetching components', err)
        });
      },
      error: (err) => console.error('Error fetching child devices', err)
    });
  }

  searchDevicesForLinking(event: any): void {
    const query = event.target.value;
    this.deviceSearchQuery = query;
    if (!query || query.length < 2) {
      this.deviceSearchResults = [];
      return;
    }
    // Search for assets excluding self and already linked
    this.http.get<any>(`/api/assets?search=${query}&size=10`).subscribe({
      next: (data) => {
        this.deviceSearchResults = (data.content || []).filter((a: any) => 
          Number(a.id) !== Number(this.linkingAsset.id) && 
          !this.linkedDevices.find(d => d.isAsset && Number(d.id) === Number(a.id))
        );
      }
    });
  }

  linkNewDevice(childAsset: any): void {
    console.log('Linking asset', childAsset.id, 'to parent', this.linkingAsset.id);
    this.http.put(`/api/assets/${childAsset.id}/link/${this.linkingAsset.id}`, {}).subscribe({
      next: () => {
        this.toast.success(`Successfully linked ${childAsset.assetTag}`);
        this.deviceSearchQuery = '';
        this.deviceSearchResults = [];
        this.showAddDeviceSearch = false;
        this.loadLinkingData(this.linkingAsset.id);
      },
      error: (err) => {
        console.error('Error linking device', err);
        this.toast.error(err.error?.message || 'Failed to link device');
      }
    });
  }

  unlinkDevice(device: any): void {
    if (!device.isAsset) return;
    
    if (confirm(`Are you sure you want to unlink ${device.assetTag}?`)) {
      this.http.put(`/api/assets/${device.id}/unlink`, {}).subscribe({
        next: () => {
          this.toast.success(`Successfully unlinked ${device.assetTag}`);
          this.loadLinkingData(this.linkingAsset.id);
        },
        error: (err) => {
          console.error('Error unlinking device', err);
          this.toast.error(err.error?.message || 'Failed to unlink device');
        }
      });
    }
  }

  closeLinkingModal(): void {
    this.showLinkingModal = false;
    this.linkingAsset = null;
    this.linkedDevices = [];
    this.showAddDeviceSearch = false;
    this.deviceSearchQuery = '';
    this.deviceSearchResults = [];
  }

  openEditFromMenu(asset: any): void {
    this.openMenuId = null;
    this.openEdit(asset);
  }

  getWarrantyExpiry(asset: any): string | null {
    if (!asset?.purchaseDate || !asset?.warrantyMonths) return null;
    const d = new Date(asset.purchaseDate);
    d.setMonth(d.getMonth() + Number(asset.warrantyMonths));
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  isWarrantyExpired(asset: any): boolean {
    if (!asset?.purchaseDate || !asset?.warrantyMonths) return false;
    const d = new Date(asset.purchaseDate);
    d.setMonth(d.getMonth() + Number(asset.warrantyMonths));
    return d < new Date();
  }

  getComponentWarrantyExpiry(c: any): string | null {
    if (!c?.installationDate || !c?.warrantyMonths) return null;
    const d = new Date(c.installationDate);
    d.setMonth(d.getMonth() + Number(c.warrantyMonths));
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  isComponentWarrantyExpired(c: any): boolean {
    if (!c?.installationDate || !c?.warrantyMonths) return false;
    const d = new Date(c.installationDate);
    d.setMonth(d.getMonth() + Number(c.warrantyMonths));
    return d < new Date();
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

  submitReplaceComponent(assetId: number): void {
    if (!this.replaceRequest.componentType) return;
    this.assetService.replaceComponent(assetId, this.replaceRequest).subscribe({
      next: (data) => {
        this.assetComponents = data;
        this.showReplaceForm = false;
        this.replaceRequest = {
          componentType: '', serialNumber: '', warrantyMonths: undefined
        };
      },
      error: (err) => { console.error(err); }
    });
  }

  badgeClass(status: string): string {
    const map: any = { 
      AVAILABLE: 'available', 
      ALLOCATED: 'allocated', 
      UNDER_MAINTENANCE: 'maintenance', 
      DAMAGED: 'damaged',
      RETIRED: 'retired', 
      LOST: 'lost' 
    };
    return 'badge badge-' + (map[status] || status.toLowerCase());
  }

  getDynamicValue(a: Asset, field: string): string {
    if (!a.dynamicAttributes) return '-';
    try {
      const attrs = typeof a.dynamicAttributes === 'string' ? JSON.parse(a.dynamicAttributes) : a.dynamicAttributes;
      return attrs[field] || '-';
    } catch (e) {
      return '-';
    }
  }

  isExpired(dateStr: string | undefined): boolean {
    if (!dateStr) return false;
    return new Date(dateStr) < new Date();
  }

  formatStatus(status: string): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase().split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  }

  formatDisposition(disposition: string): string {
    if (!disposition) return '';
    return disposition.replace(/_/g, ' ').toLowerCase().split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  }

  openReplaceComponent(asset: any): void {
    this.replacingAsset = asset;
    this.replaceForm = {
      componentType: '',
      serialNumber: '',
      warrantyMonths: null,
      oldComponentDisposition: ''
    };
    this.showReplaceModal = true;
  }

  closeReplaceModal(): void {
    this.showReplaceModal = false;
    this.replacingAsset = null;
  }

  submitReplaceModal(): void {
    if (!this.replaceForm.componentType) {
      this.toast.error('Please select a component type');
      return;
    }
    this.replaceSaving = true;
    const payload: any = {
      componentType: this.replaceForm.componentType,
      serialNumber:  this.replaceForm.serialNumber || null,
      warrantyMonths: this.replaceForm.warrantyMonths || null
    };
    if (this.replaceForm.oldComponentDisposition) {
      payload.oldComponentDisposition = this.replaceForm.oldComponentDisposition;
    }
    this.assetService.replaceComponent(this.replacingAsset.id, payload).subscribe({
      next: () => {
        this.replaceSaving = false;
        this.closeReplaceModal();
        this.toast.success('Component replaced successfully');
        this.loadAssets(); // Refresh assets table
      },
      error: (err: any) => {
        this.replaceSaving = false;
        this.toast.error(err.error?.message || 'Failed to replace component');
      }
    });
  }
}
