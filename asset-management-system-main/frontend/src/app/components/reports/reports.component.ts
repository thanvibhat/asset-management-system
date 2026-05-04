import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../services/dashboard.service';
import { ReportService } from '../../services/report.service';
import { DashboardStats, MaintenanceAnalytics, ProcurementAnalytics, AssetMetricsDto } from '../../models/models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
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
  
  loading = true;

  constructor(
    private dashboardService: DashboardService,
    private reportService: ReportService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAllData();
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
      topPerformers: this.reportService.getTopPerformers().pipe(catchError(() => of([]))),
      highCost: this.reportService.getHighCostAssets().pipe(catchError(() => of([]))),
      frequent: this.reportService.getFrequentRepairs().pipe(catchError(() => of([]))),
      poorValue: this.reportService.getPoorValueAssets().pipe(catchError(() => of([])))
    }).subscribe({
      next: data => {
        this.stats = data.stats;
        this.maintenanceAnalytics = data.maintenance;
        this.procurementAnalytics = data.procurement;
        this.topPerformers = data.topPerformers;
        this.highCostAssets = data.highCost;
        this.frequentRepairs = data.frequent;
        this.poorValueAssets = data.poorValue;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  askAi(question: string) {
    this.router.navigate(['/llm-reports'], { queryParams: { q: question } });
  }

  download(endpoint: string, filename: string): void {
    this.reportService.downloadReport(endpoint, filename);
  }

  getEntries(obj: { [key: string]: number } | undefined): [string, number][] {
    return obj ? Object.entries(obj) : [];
  }
}
