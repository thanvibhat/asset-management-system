import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MasterService {
  constructor(private http: HttpClient) {}

  // Locations
  getLocations(page: number = 0, size: number = 10): Observable<any> { return this.http.get<any>(`/api/master/locations?page=${page}&size=${size}`); }
  getLocationsList(): Observable<any[]> { return this.http.get<any[]>('/api/master/locations/all'); }
  searchLocations(q: string): Observable<any[]> { return this.http.get<any[]>(`/api/master/locations/search?q=${q}`); }
  createLocation(data: {name: string, description: string}): Observable<any> { return this.http.post('/api/master/locations', data); }
  updateLocation(id: number, data: any): Observable<any> { return this.http.put(`/api/master/locations/${id}`, data); }
  deleteLocation(id: number): Observable<void> { return this.http.delete<void>(`/api/master/locations/${id}`); }

  // Manufacturers
  getManufacturers(page: number = 0, size: number = 10): Observable<any> { return this.http.get<any>(`/api/master/manufacturers?page=${page}&size=${size}`); }
  getManufacturersList(): Observable<any[]> { return this.http.get<any[]>('/api/master/manufacturers/all'); }
  searchManufacturers(q: string): Observable<any[]> { return this.http.get<any[]>(`/api/master/manufacturers/search?q=${q}`); }
  createManufacturer(data: {name: string, website: string}): Observable<any> { return this.http.post('/api/master/manufacturers', data); }
  updateManufacturer(id: number, data: any): Observable<any> { return this.http.put(`/api/master/manufacturers/${id}`, data); }
  deleteManufacturer(id: number): Observable<void> { return this.http.delete<void>(`/api/master/manufacturers/${id}`); }

  // Product Master
  getProducts(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`/api/products?page=${page}&size=${size}`);
  }
  getProductsList(): Observable<any[]> {
    return this.http.get<any[]>('/api/products/all');
  }
  getProduct(id: number): Observable<any> { return this.http.get<any>(`/api/products/${id}`); }
  createProduct(data: any): Observable<any> { return this.http.post('/api/products', data); }
  updateProduct(id: number, data: any): Observable<any> { return this.http.put(`/api/products/${id}`, data); }
  deleteProduct(id: number): Observable<void> { return this.http.delete<void>(`/api/products/${id}`); }
}
