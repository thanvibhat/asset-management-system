import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardService } from '../../services/dashboard.service';
import { ReportService } from '../../services/report.service';
import { AssetService } from '../../services/asset.service';
import { DashboardStats, MaintenanceAnalytics, ProcurementAnalytics, AssetMetricsDto, AssetCategory, Asset } from '../../models/models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css']
})
export class ReportsComponent implements OnInit {
  stats: DashboardStats | null = null;
  maintenanceAnalytics: MaintenanceAnalytics | null = null;
  procurementAnalytics: ProcurementAnalytics | null = null;
  
  topPerformers: AssetMetricsDto[] = [];
  highCostAssets: AssetMetricsDto[] = [];
  frequentRepairs: AssetMetricsDto[] = [];
  poorValueAssets: AssetMetricsDto[] = [];
  
  categories: AssetCategory[] = [];
  filterFromDate = '';
  filterToDate = '';
  filterCategoryId: number | null = null;
  loading = true;
  filteringMetrics = false;
  activeWorkspaceTab: 'inventory' | 'spend' | 'performance' = 'inventory';
  allAssets: Asset[] = [];
  warrantyFilter: 'all' | 'active' | 'expired' = 'all';

  constructor(
    private dashboardService: DashboardService,
    private reportService: ReportService,
    private assetService: AssetService,
    private router: Router
  ) {}

  setWorkspaceTab(tab: 'inventory' | 'spend' | 'performance'): void {
    this.activeWorkspaceTab = tab;
  }

  ngOnInit(): void {
    this.loadAllData();
    this.loadCategories();
  }

  loadCategories(): void {
    this.assetService.getCategories().subscribe({
      next: cats => this.categories = cats,
      error: () => this.categories = []
    });
  }

  loadAllData(): void {
    this.loading = true;
    forkJoin({
      stats: this.dashboardService.getStats().pipe(catchError(() => of({
        totalAssets: 0, allocatedAssets: 0, availableAssets: 0, maintenanceAssets: 0, disposedAssets: 0,
        totalValue: 0, categoryDistribution: {}, statusDistribution: {}
      } as DashboardStats))),
      maintenance: this.reportService.getMaintenanceAnalytics().pipe(catchError(() => of(null))),
      procurement: this.reportService.getProcurementAnalytics().pipe(catchError(() => of(null))),
      assets: this.assetService.getAssets({ size: 1000 }).pipe(catchError(() => of({ content: [] } as any)))
    }).subscribe({
      next: data => {
        this.stats = data.stats;
        this.maintenanceAnalytics = data.maintenance;
        this.procurementAnalytics = data.procurement;
        this.allAssets = data.assets?.content || [];
        this.loading = false;
        this.applyFilters(); // Fetch metric lists with filter constraints
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteringMetrics = true;
    const fromStr = this.filterFromDate || undefined;
    const toStr = this.filterToDate || undefined;
    const catId = this.filterCategoryId || undefined;

    forkJoin({
      topPerformers: this.reportService.getTopPerformers(fromStr, toStr, catId).pipe(catchError(() => of([]))),
      highCost: this.reportService.getHighCostAssets(fromStr, toStr, catId).pipe(catchError(() => of([]))),
      frequent: this.reportService.getFrequentRepairs(fromStr, toStr, catId).pipe(catchError(() => of([]))),
      poorValue: this.reportService.getPoorValueAssets(fromStr, toStr, catId).pipe(catchError(() => of([])))
    }).subscribe({
      next: data => {
        // Show only the top 3 assets on this page to prevent clutter
        this.topPerformers = data.topPerformers ? data.topPerformers.slice(0, 3) : [];
        this.highCostAssets = data.highCost ? data.highCost.slice(0, 3) : [];
        this.frequentRepairs = data.frequent ? data.frequent.slice(0, 3) : [];
        this.poorValueAssets = data.poorValue ? data.poorValue.slice(0, 3) : [];
        this.filteringMetrics = false;
      },
      error: () => {
        this.filteringMetrics = false;
      }
    });
  }

  resetFilters(): void {
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterCategoryId = null;
    this.applyFilters();
  }

  onAssetClick(rankingType: string): void {
    const queryParams: any = { ranking: rankingType };
    if (this.filterFromDate) queryParams.fromDate = this.filterFromDate;
    if (this.filterToDate) queryParams.toDate = this.filterToDate;
    if (this.filterCategoryId) queryParams.categoryId = this.filterCategoryId;

    this.router.navigate(['/assets'], { queryParams });
  }

  setWarrantyFilter(filter: 'all' | 'active' | 'expired'): void {
    this.warrantyFilter = filter;
  }

  isAssetUnderWarranty(asset: Asset): boolean {
    if (!asset.warrantyExpiryDate) return false;
    const expiry = new Date(asset.warrantyExpiryDate);
    const today = new Date();
    expiry.setHours(0,0,0,0);
    today.setHours(0,0,0,0);
    return expiry >= today;
  }

  getFilteredWarrantyAssets(): Asset[] {
    if (!this.allAssets) return [];
    return this.allAssets.filter(asset => {
      const isUnder = this.isAssetUnderWarranty(asset);
      if (this.warrantyFilter === 'active') {
        return isUnder;
      } else if (this.warrantyFilter === 'expired') {
        return !isUnder;
      }
      return true;
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

  badgeClass(status: string): string {
    return 'badge badge-' + (status || '').toLowerCase();
  }

  formatStatus(status: string): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase().split(' ').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
  }

  activeDropdown: string | null = null;
  activeInfoTooltip: string | null = null;

  toggleDownloadDropdown(section: string, event: MouseEvent): void {
    event.stopPropagation();
    this.activeDropdown = this.activeDropdown === section ? null : section;
  }

  toggleInfoTooltip(section: string, event: MouseEvent): void {
    event.stopPropagation();
    this.activeInfoTooltip = this.activeInfoTooltip === section ? null : section;
  }

  @HostListener('document:click')
  closeDropdowns(): void {
    this.activeDropdown = null;
    this.activeInfoTooltip = null;
  }

  downloadFormat(section: string, format: string): void {
    this.activeDropdown = null;
    
    let endpoint = '';
    let filename = '';
    let reportTitle = '';
    let data: any[] = [];
    
    if (section === 'topPerformers') {
      endpoint = 'top-performers';
      filename = 'top_performers';
      reportTitle = 'Best Performing Assets';
      data = this.topPerformers;
    } else if (section === 'highCost') {
      endpoint = 'maintenance-costs';
      filename = 'maintenance_report';
      reportTitle = 'Highest Maintenance Cost Assets';
      data = this.highCostAssets;
    } else if (section === 'frequent') {
      endpoint = 'frequent-repairs';
      filename = 'frequent_repairs';
      reportTitle = 'Frequent Repairs Assets';
      data = this.frequentRepairs;
    } else if (section === 'poorValue') {
      endpoint = 'poor-value-assets';
      filename = 'poor_value_assets';
      reportTitle = 'Poor Value Assets';
      data = this.poorValueAssets;
    }

    if (format === 'xls') {
      this.reportService.downloadReport(endpoint, `${filename}.xls`);
    } else if (format === 'pdf') {
      this.generatePdfReport(reportTitle, data);
    }
  }

  generatePdfReport(title: string, data: any[]): void {
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;

    let tableHeaders = '';
    let tableRows = '';

    if (title.includes('Best Performing')) {
      tableHeaders = `
        <th>Asset Tag</th>
        <th>Asset Name</th>
        <th>Category</th>
        <th>Serial Number</th>
        <th>Manufacturer / Model</th>
        <th>Location</th>
        <th>Status</th>
        <th style="text-align: right;">Value Retention %</th>
        <th style="text-align: right;">Maintenance Ratio</th>
      `;
      tableRows = data.map(a => `
        <tr>
          <td><strong>${a.assetTag || '-'}</strong></td>
          <td>${a.assetName || '-'}</td>
          <td>${a.category || '-'}</td>
          <td>${a.serialNumber || '-'}</td>
          <td>${(a.manufacturer || '') + (a.manufacturer && a.model ? ' / ' : '') + (a.model || '') || '-'}</td>
          <td>${a.location || '-'}</td>
          <td><span style="font-size: 11px; font-weight: 600; color: #10b981;">${a.status || 'AVAILABLE'}</span></td>
          <td style="text-align: right;">${a.currentValueRetentionPct ? a.currentValueRetentionPct.toFixed(1) + '%' : '-'}</td>
          <td style="text-align: right;">${a.maintenanceToPurchaseRatio ? a.maintenanceToPurchaseRatio.toFixed(2) : '-'}</td>
        </tr>
      `).join('');
    } else if (title.includes('Highest Maintenance')) {
      tableHeaders = `
        <th>Asset Tag</th>
        <th>Asset Name</th>
        <th>Category</th>
        <th>Serial Number</th>
        <th>Manufacturer / Model</th>
        <th>Location</th>
        <th>Status</th>
        <th style="text-align: right;">Total Maint. Cost</th>
        <th style="text-align: right;">Maint. Ratio</th>
      `;
      tableRows = data.map(a => `
        <tr>
          <td><strong>${a.assetTag || '-'}</strong></td>
          <td>${a.assetName || '-'}</td>
          <td>${a.category || '-'}</td>
          <td>${a.serialNumber || '-'}</td>
          <td>${(a.manufacturer || '') + (a.manufacturer && a.model ? ' / ' : '') + (a.model || '') || '-'}</td>
          <td>${a.location || '-'}</td>
          <td><span style="font-size: 11px; font-weight: 600; color: #2563eb;">${a.status || 'AVAILABLE'}</span></td>
          <td style="text-align: right;">₹${a.totalMaintenanceCost ? a.totalMaintenanceCost.toLocaleString('en-IN') : '-'}</td>
          <td style="text-align: right;">${a.maintenanceToPurchaseRatio ? a.maintenanceToPurchaseRatio.toFixed(2) : '-'}</td>
        </tr>
      `).join('');
    } else if (title.includes('Frequent Repairs')) {
      tableHeaders = `
        <th>Asset Tag</th>
        <th>Asset Name</th>
        <th>Category</th>
        <th>Serial Number</th>
        <th>Manufacturer / Model</th>
        <th>Location</th>
        <th>Status</th>
        <th style="text-align: right;">Repairs Count</th>
        <th style="text-align: right;">Avg Days Between Repairs</th>
      `;
      tableRows = data.map(a => `
        <tr>
          <td><strong>${a.assetTag || '-'}</strong></td>
          <td>${a.assetName || '-'}</td>
          <td>${a.category || '-'}</td>
          <td>${a.serialNumber || '-'}</td>
          <td>${(a.manufacturer || '') + (a.manufacturer && a.model ? ' / ' : '') + (a.model || '') || '-'}</td>
          <td>${a.location || '-'}</td>
          <td><span style="font-size: 11px; font-weight: 600; color: #eab308;">${a.status || 'AVAILABLE'}</span></td>
          <td style="text-align: right;">${a.correctiveRepairCount || 0}</td>
          <td style="text-align: right;">${a.averageDaysBetweenRepairs ? a.averageDaysBetweenRepairs.toFixed(0) : 'N/A'}</td>
        </tr>
      `).join('');
    } else if (title.includes('Poor Value')) {
      tableHeaders = `
        <th>Asset Tag</th>
        <th>Asset Name</th>
        <th>Category</th>
        <th>Serial Number</th>
        <th>Manufacturer / Model</th>
        <th>Location</th>
        <th>Status</th>
        <th style="text-align: right;">Maint. Ratio</th>
        <th style="text-align: center;">Recommended Action</th>
      `;
      tableRows = data.map(a => `
        <tr>
          <td><strong>${a.assetTag || '-'}</strong></td>
          <td>${a.assetName || '-'}</td>
          <td>${a.category || '-'}</td>
          <td>${a.serialNumber || '-'}</td>
          <td>${(a.manufacturer || '') + (a.manufacturer && a.model ? ' / ' : '') + (a.model || '') || '-'}</td>
          <td>${a.location || '-'}</td>
          <td><span style="font-size: 11px; font-weight: 600; color: #ef4444;">${a.status || 'AVAILABLE'}</span></td>
          <td style="text-align: right; color: #dc2626; font-weight: 600;">${a.maintenanceToPurchaseRatio ? a.maintenanceToPurchaseRatio.toFixed(2) : '-'}</td>
          <td style="text-align: center;">
            <span style="padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; background: ${a.maintenanceToPurchaseRatio > 0.8 ? '#fee2e2; color: #dc2626;' : '#fef3c7; color: #d97706;'}">
              ${a.maintenanceToPurchaseRatio > 0.8 ? 'REPLACE' : 'REVIEW'}
            </span>
          </td>
        </tr>
      `).join('');
    }

    const htmlContent = `
      <html>
        <head>
          <title>${title} - Report</title>
          <style>
            @page {
              size: A4 landscape;
              margin: 15mm;
            }
            body {
              font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
              color: #0f172a;
              padding: 20px;
              line-height: 1.5;
            }
            .header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              border-bottom: 2px solid #2563eb;
              padding-bottom: 20px;
              margin-bottom: 30px;
            }
            .brand-title {
              font-size: 26px;
              font-weight: 700;
              color: #2563eb;
              letter-spacing: -0.025em;
            }
            .subtitle {
              font-size: 12px;
              color: #64748b;
              text-align: right;
            }
            .report-title {
              font-size: 20px;
              font-weight: 600;
              margin-bottom: 20px;
              color: #1e293b;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              margin-bottom: 30px;
            }
            th {
              background: #f1f5f9;
              color: #475569;
              font-size: 11px;
              font-weight: 600;
              text-transform: uppercase;
              letter-spacing: 0.05em;
              padding: 10px 12px;
              border-bottom: 1px solid #cbd5e1;
              text-align: left;
            }
            td {
              padding: 10px 12px;
              font-size: 13px;
              border-bottom: 1px solid #e2e8f0;
              color: #334155;
            }
            tr:last-child td {
              border-bottom: none;
            }
            .footer {
              margin-top: 50px;
              text-align: center;
              font-size: 11px;
              color: #94a3b8;
              border-top: 1px solid #e2e8f0;
              padding-top: 20px;
            }
            @media print {
              body { padding: 0; }
              .no-print { display: none; }
            }
          </style>
        </head>
        <body>
          <div class="header">
            <div>
              <div class="brand-title">AssetIQ</div>
              <div style="font-size: 12px; color: #64748b;">Enterprise Asset Intelligence Suite</div>
            </div>
            <div class="subtitle">
              <div><strong>Generated Date:</strong> ${new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })}</div>
              <div>CONFIDENTIAL REPORT</div>
            </div>
          </div>
          
          <div class="report-title">${title}</div>
          
          <table>
            <thead>
              <tr>
                ${tableHeaders}
              </tr>
            </thead>
            <tbody>
              ${tableRows}
            </tbody>
          </table>
          
          <div class="footer">
            AssetIQ System Administrator Report &copy; ${new Date().getFullYear()} - All Rights Reserved.
          </div>
          
          <script>
            window.onload = function() {
              window.print();
              setTimeout(function() { window.close(); }, 500);
            };
          </script>
        </body>
      </html>
    `;

    printWindow.document.open();
    printWindow.document.write(htmlContent);
    printWindow.document.close();
  }

  askAi(question: string) {
    this.router.navigate(['/llm-reports'], { queryParams: { q: question } });
  }

  onCategoryClick(categoryName: string): void {
    this.router.navigate(['/assets'], { queryParams: { category: categoryName } });
  }

  onStatusClick(statusName: string): void {
    this.router.navigate(['/assets'], { queryParams: { status: statusName } });
  }

  download(endpoint: string, filename: string): void {
    this.reportService.downloadReport(endpoint, filename);
  }

  getEntries(obj: { [key: string]: number } | undefined): [string, number][] {
    return obj ? Object.entries(obj) : [];
  }
}
