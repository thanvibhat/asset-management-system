import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReportService {
  constructor(private http: HttpClient) {}

  downloadReport(endpoint: string, filename: string): void {
    this.http.get(`/api/reports/export/${endpoint}`, { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  getMaintenanceAnalytics(): Observable<any> {
    return this.http.get('/api/reports/analytics/maintenance');
  }

  getProcurementAnalytics(): Observable<any> {
    return this.http.get('/api/reports/analytics/procurement');
  }

  getTopPerformers(): Observable<any> {
    return this.http.get('/api/reports/top-performers');
  }

  getHighCostAssets(): Observable<any> {
    return this.http.get('/api/reports/high-cost-assets');
  }

  getFrequentRepairs(): Observable<any> {
    return this.http.get('/api/reports/frequent-repairs');
  }

  getPoorValueAssets(): Observable<any> {
    return this.http.get('/api/reports/poor-value-assets');
  }
}
