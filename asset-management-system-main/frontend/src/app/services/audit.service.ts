import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly API = '/api/audit';

  constructor(private http: HttpClient) {}

  getLogs(page: number = 0, size: number = 20): Observable<Page<any>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<any>>(this.API, { params });
  }
}
