import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification, Page } from '../models/models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly API = '/api/notifications';

  constructor(private http: HttpClient) {}

  getNotifications(page: number = 0, size: number = 10): Observable<Page<Notification>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Notification>>(this.API, { params });
  }

  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.API}/unread-count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.post<void>(`${this.API}/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.post<void>(`${this.API}/read-all`, {});
  }
}
