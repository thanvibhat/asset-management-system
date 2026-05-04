import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Allocation, MaintenanceRecord, Page } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AllocationService {
  private readonly API = '/api/allocations';

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 10): Observable<Page<Allocation>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<Page<Allocation>>(this.API, { params });
  }

  allocate(data: { assetId: number; userId: number; expectedReturnDate?: string; notes?: string }): Observable<Allocation> {
    return this.http.post<Allocation>(this.API, data);
  }

  returnAsset(id: number): Observable<Allocation> {
    return this.http.put<Allocation>(`${this.API}/${id}/return`, {});
  }
}

@Injectable({ providedIn: 'root' })
export class MaintenanceService {
  private readonly API = '/api/maintenance';

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 10, status?: string): Observable<Page<MaintenanceRecord>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (status) params = params.set('status', status);
    return this.http.get<Page<MaintenanceRecord>>(this.API, { params });
  }

  create(data: any): Observable<MaintenanceRecord> { return this.http.post<MaintenanceRecord>(this.API, data); }

  update(id: number, data: any): Observable<MaintenanceRecord> { return this.http.put<MaintenanceRecord>(`${this.API}/${id}`, data); }
}
