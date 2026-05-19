import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Asset, AssetCategory, DashboardStats, Page } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AssetService {
  private readonly API = '/api/assets';

  constructor(private http: HttpClient) {}

  getAssets(filters: { status?: string; categoryId?: number; search?: string; page?: number; size?: number } = {}): Observable<Page<Asset>> {
    let params = new HttpParams();
    if (filters.status) params = params.set('status', filters.status);
    if (filters.categoryId) params = params.set('categoryId', filters.categoryId.toString());
    if (filters.search) params = params.set('search', filters.search);
    params = params.set('page', (filters.page ?? 0).toString());
    params = params.set('size', (filters.size ?? 10).toString());
    return this.http.get<Page<Asset>>(this.API, { params });
  }

  getAsset(id: number): Observable<Asset> { return this.http.get<Asset>(`${this.API}/${id}`); }

  createAsset(data: any): Observable<Asset> { return this.http.post<Asset>(this.API, data); }

  updateAsset(id: number, data: any): Observable<Asset> { return this.http.put<Asset>(`${this.API}/${id}`, data); }

  deleteAsset(id: number): Observable<void> { return this.http.delete<void>(`${this.API}/${id}`); }

  transferAsset(id: number, newLocation: string): Observable<Asset> {
    let params = new HttpParams().set('newLocation', newLocation);
    return this.http.put<Asset>(`${this.API}/${id}/transfer`, null, { params });
  }

  getStats(): Observable<DashboardStats> { return this.http.get<DashboardStats>(`${this.API}/dashboard/stats`); }

  getCategories(): Observable<AssetCategory[]> { return this.http.get<AssetCategory[]>(`${this.API}/categories`); }
  
  exportAssets(): Observable<Blob> {
    return this.http.get(`${this.API}/export`, { responseType: 'blob' });
  }

  downloadExcelTemplate(): Observable<Blob> {
    return this.http.get(`${this.API}/excel-template`, { responseType: 'blob' });
  }

  getComponents(assetId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/${assetId}/components`);
  }

  getActiveComponents(assetId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/${assetId}/components/active`);
  }

  replaceComponent(assetId: number, data: {
    componentType: string;
    serialNumber?: string | null;
    warrantyMonths?: number | null;
    oldComponentDisposition?: string | null;
  }): Observable<any[]> {
    return this.http.post<any[]>(
      `${this.API}/${assetId}/components/replace`, data
    );
  }

  getAssetHistory(assetId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/${assetId}/history`);
  }
}
