import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, Page } from '../models/models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly API = '/api/users';
  constructor(private http: HttpClient) {}

    getAll(page = 0, size = 10): Observable<Page<User>> {
      const params = new HttpParams().set('page', page).set('size', size);
      return this.http.get<Page<User>>(this.API, { params });
    }
  
    searchUsers(q: string): Observable<User[]> {
      const params = new HttpParams().set('q', q);
      return this.http.get<User[]>(`${this.API}/search`, { params });
    }

  update(id: number, data: any): Observable<User> {
    return this.http.put<User>(`${this.API}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  toggleStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API}/${id}/toggle`, {});
  }

  create(data: any): Observable<User> {
    return this.http.post<User>(this.API, data);
  }

  resetPassword(id: number): Observable<void> {
    return this.http.post<void>(`${this.API}/${id}/reset-password`, {});
  }
}
