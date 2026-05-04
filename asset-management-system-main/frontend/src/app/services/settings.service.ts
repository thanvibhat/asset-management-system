import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  constructor(private http: HttpClient) {}

  // Roles
  getRoles(): Observable<any[]> { return this.http.get<any[]>('/api/roles'); }
  getPermissions(): Observable<any[]> { return this.http.get<any[]>('/api/roles/permissions'); }
  createRole(data: any): Observable<any> { return this.http.post<any>('/api/roles', data); }
  updateRole(id: number, data: any): Observable<any> { return this.http.put<any>(`/api/roles/${id}`, data); }
  deleteRole(id: number): Observable<void> { return this.http.delete<void>(`/api/roles/${id}`); }
  assignPermissions(roleId: number, permissionIds: number[]): Observable<any> {
    return this.http.put<any>(`/api/roles/${roleId}/permissions`, { permissionIds });
  }

  // Categories
  getCategoryDetails(): Observable<any[]> { return this.http.get<any[]>('/api/assets/categories/details'); }
  createCategory(data: any): Observable<any> { return this.http.post<any>('/api/assets/categories', data); }
  updateCategory(id: number, data: any): Observable<any> { return this.http.put<any>(`/api/assets/categories/${id}`, data); }
  deleteCategory(id: number): Observable<void> { return this.http.delete<void>(`/api/assets/categories/${id}`); }
  updateSchema(id: number, schema: string): Observable<any> {
    return this.http.put<any>(`/api/assets/categories/${id}/schema`, { attributeSchema: schema });
  }
}
