import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  message: string;
  type: 'success' | 'error' | 'info';
  id: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toasts: Toast[] = [];
  private toasts$ = new BehaviorSubject<Toast[]>([]);
  private counter = 0;

  get notifications$() {
    return this.toasts$.asObservable();
  }

  show(message: string, type: 'success' | 'error' | 'info' = 'info') {
    const id = this.counter++;
    const toast: Toast = { message, type, id };
    this.toasts.push(toast);
    this.toasts$.next([...this.toasts]);

    // Auto-remove after 5 seconds
    setTimeout(() => this.remove(id), 5000);
  }

  success(message: string) { this.show(message, 'success'); }
  error(message: string) { this.show(message, 'error'); }
  info(message: string) { this.show(message, 'info'); }

  remove(id: number) {
    this.toasts = this.toasts.filter(t => t.id !== id);
    this.toasts$.next([...this.toasts]);
  }
}
