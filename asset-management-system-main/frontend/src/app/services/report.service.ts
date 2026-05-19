import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReportService {
  constructor(private http: HttpClient) {}

  private buildParams(fromDate?: string, toDate?: string, categoryId?: number): HttpParams {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (categoryId) params = params.set('categoryId', categoryId.toString());
    return params;
  }

  downloadReport(endpoint: string, filename: string): void {
    console.log(`[ReportService] Initiating download from /api/reports/export/${endpoint} to save as ${filename}`);
    this.http.get(`/api/reports/export/${endpoint}`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        console.log(`[ReportService] Successfully fetched blob of size ${blob.size} bytes`);
        
        // Convert CSV blob to Excel MIME type if downloading as .xls to bypass modern browser security blocks
        let downloadBlob = blob;
        if (filename.endsWith('.xls')) {
          downloadBlob = new Blob([blob], { type: 'application/vnd.ms-excel' });
          console.log(`[ReportService] Converted blob to application/vnd.ms-excel for .xls format compatibility`);
        }

        const url = window.URL.createObjectURL(downloadBlob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        console.log(`[ReportService] Completed download process for ${filename}`);
      },
      error: (err) => {
        console.error(`[ReportService] Failed to download report from ${endpoint}:`, err);
      }
    });
  }

  getMaintenanceAnalytics(): Observable<any> {
    return this.http.get('/api/reports/analytics/maintenance');
  }

  getProcurementAnalytics(): Observable<any> {
    return this.http.get('/api/reports/analytics/procurement');
  }

  getTopPerformers(fromDate?: string, toDate?: string, categoryId?: number): Observable<any> {
    const params = this.buildParams(fromDate, toDate, categoryId);
    return this.http.get('/api/reports/top-performers', { params });
  }

  getHighCostAssets(fromDate?: string, toDate?: string, categoryId?: number): Observable<any> {
    const params = this.buildParams(fromDate, toDate, categoryId);
    return this.http.get('/api/reports/high-cost-assets', { params });
  }

  getFrequentRepairs(fromDate?: string, toDate?: string, categoryId?: number): Observable<any> {
    const params = this.buildParams(fromDate, toDate, categoryId);
    return this.http.get('/api/reports/frequent-repairs', { params });
  }

  getPoorValueAssets(fromDate?: string, toDate?: string, categoryId?: number): Observable<any> {
    const params = this.buildParams(fromDate, toDate, categoryId);
    return this.http.get('/api/reports/poor-value-assets', { params });
  }
}
