import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { LoginResponse, User } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = '/api/auth';
  private readonly TOKEN_KEY = 'jwt_token';
  private readonly USER_KEY = 'current_user';

  private currentUserSubject = new BehaviorSubject<LoginResponse | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API}/login`, { username, password }).pipe(
      tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.USER_KEY, JSON.stringify(res));
        this.currentUserSubject.next(res);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null { return localStorage.getItem(this.TOKEN_KEY); }

  get currentUser(): LoginResponse | null { return this.currentUserSubject.value; }

  get isLoggedIn(): boolean { return !!this.getToken(); }

  get roles(): string[] { return this.currentUser?.roles || []; }
  get permissions(): string[] { return this.currentUser?.permissions || []; }

  hasRole(role: string): boolean { return this.roles.includes(role); }
  hasPermission(permission: string): boolean { return this.permissions.includes(permission); }

  canWrite(): boolean { return this.hasPermission('ASSET_UPDATE') || this.hasRole('ROLE_ADMIN') || this.hasRole('ROLE_MANAGER'); }
  isAdmin(): boolean { return this.hasRole('ROLE_ADMIN'); }

  getUsers(): Observable<User[]> { return this.http.get<User[]>(`${this.API}/users`); }

  createUser(data: any): Observable<User> { return this.http.post<User>(`${this.API}/users`, data); }

  private loadUser(): LoginResponse | null {
    const json = localStorage.getItem(this.USER_KEY);
    return json ? JSON.parse(json) : null;
  }
}
