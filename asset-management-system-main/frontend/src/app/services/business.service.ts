import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Vendor, Procurement, DashboardStats } from '../models/business.model';

@Injectable({
  providedIn: 'root'
})
export class BusinessService {
  private apiUrl = '/api';

  constructor(private http: HttpClient) {}

  // Vendors
  getVendors(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/vendors?page=${page}&size=${size}`);
  }
  getVendorsList(): Observable<Vendor[]> {
    return this.http.get<Vendor[]>(`${this.apiUrl}/vendors/all`);
  }
  getVendor(id: number): Observable<Vendor> {
    return this.http.get<Vendor>(`${this.apiUrl}/vendors/${id}`);
  }
  createVendor(vendor: Vendor): Observable<Vendor> {
    return this.http.post<Vendor>(`${this.apiUrl}/vendors`, vendor);
  }
  updateVendor(id: number, vendor: Vendor): Observable<Vendor> {
    return this.http.put<Vendor>(`${this.apiUrl}/vendors/${id}`, vendor);
  }
  deleteVendor(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/vendors/${id}`);
  }

  // Procurement
  getProcurements(): Observable<Procurement[]> {
    return this.http.get<Procurement[]>(`${this.apiUrl}/procurement`);
  }
  getProcurement(id: number): Observable<Procurement> {
    return this.http.get<Procurement>(`${this.apiUrl}/procurement/${id}`);
  }
  createProcurement(procurement: Procurement): Observable<Procurement> {
    return this.http.post<Procurement>(`${this.apiUrl}/procurement`, procurement);
  }
  updateProcurement(id: number, procurement: Procurement): Observable<Procurement> {
    return this.http.put<Procurement>(`${this.apiUrl}/procurement/${id}`, procurement);
  }
  deleteProcurement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/procurement/${id}`);
  }

  // Reports
  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/reports/dashboard-stats`);
  }
}
