import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardStats } from '../../models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats = {
    totalAssets: 0,
    allocatedAssets: 0,
    availableAssets: 0,
    maintenanceAssets: 0,
    totalValue: 0,
    categoryDistribution: {},
    statusDistribution: {}
  };
  loading = true;
  today = new Date();

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.dashboardService.getStats().subscribe({
      next: s => { if (s) this.stats = s; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  getUtilization(): number {
    if (!this.stats || !this.stats.totalAssets) return 0;
    return Math.round((this.stats.allocatedAssets / this.stats.totalAssets) * 100);
  }

  getChartSegments(): Array<{label:string, value:number, percent:number, color:string, dashArray:string, dashOffset:string}> {
    if (!this.stats) return [];
    const circumference = 2 * Math.PI * 70; // 439.8
    const total = this.stats.totalAssets || 1;
    const segments = [
      { label: 'Available',    value: this.stats.availableAssets,   color: '#059669' },
      { label: 'Allocated',    value: this.stats.allocatedAssets,   color: '#0284c7' },
      { label: 'Under maintenance',  value: this.stats.maintenanceAssets, color: '#d97706' },
      { label: 'Other',        value: Math.max(0, total - this.stats.availableAssets - this.stats.allocatedAssets - this.stats.maintenanceAssets), color: '#dc2626' },
    ];
    let cumulativePercent = 0;
    return segments.map(s => {
      const percent = (s.value / total) * 100;
      const dashArray = `${(percent / 100) * circumference} ${circumference}`;
      const dashOffset = `${-((cumulativePercent / 100) * circumference)}`;
      cumulativePercent += percent;
      return { ...s, percent: Math.round(percent), dashArray, dashOffset };
    });
  }

  getCategoryEntries(): Array<{name:string, count:number, pct:number, color:string}> {
    if (!this.stats?.categoryDistribution) return [];
    const colors = ['#2563eb','#059669','#0284c7','#7c3aed','#d97706','#dc2626'];
    const total = this.stats.totalAssets || 1;
    return Object.entries(this.stats.categoryDistribution)
      .map(([name, count], i) => ({
        name,
        count: count as number,
        pct: Math.round(((count as number) / total) * 100),
        color: colors[i % colors.length]
      }))
      .sort((a, b) => b.count - a.count);
  }

  getStackedBarSegments(): Array<{label:string, pct:number, color:string}> {
    if (!this.stats) return [];
    const t = this.stats.totalAssets || 1;
    const other = Math.max(0, t - this.stats.availableAssets - this.stats.allocatedAssets - this.stats.maintenanceAssets);
    return [
      { label: 'Available',   pct: (this.stats.availableAssets / t) * 100,   color: '#059669' },
      { label: 'Allocated',   pct: (this.stats.allocatedAssets / t) * 100,   color: '#0284c7' },
      { label: 'Under maintenance', pct: (this.stats.maintenanceAssets / t) * 100, color: '#d97706' },
      { label: 'Other',       pct: (other / t) * 100,                         color: '#94a3b8' },
    ];
  }
}
