import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  message: string;
  type: 'success' | 'error' | 'info';
  id: number;
  actionLabel?: string;
  action?: () => void;
  onRemove?: () => void;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toasts: Toast[] = [];
  private toasts$ = new BehaviorSubject<Toast[]>([]);
  private counter = 0;

  get notifications$() {
    return this.toasts$.asObservable();
  }

  show(message: string, type: 'success' | 'error' | 'info' = 'info', actionLabel?: string, action?: () => void, onRemove?: () => void) {
    const id = this.counter++;
    const toast: Toast = { message, type, id, actionLabel, action, onRemove };
    this.toasts.push(toast);
    this.toasts$.next([...this.toasts]);

    // Auto-remove after 5 seconds
    setTimeout(() => this.remove(id), 5000);
  }

  success(message: string, actionLabel?: string, action?: () => void, onRemove?: () => void) { 
    this.show(message, 'success', actionLabel, action, onRemove); 
  }
  error(message: string, actionLabel?: string, action?: () => void, onRemove?: () => void) { 
    this.show(message, 'error', actionLabel, action, onRemove); 
  }
  info(message: string, actionLabel?: string, action?: () => void, onRemove?: () => void) { 
    this.show(message, 'info', actionLabel, action, onRemove); 
  }

  remove(id: number) {
    const toast = this.toasts.find(t => t.id === id);
    if (toast?.onRemove) {
      toast.onRemove();
    }
    this.toasts = this.toasts.filter(t => t.id !== id);
    this.toasts$.next([...this.toasts]);
  }
}
