import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div *ngFor="let toast of toasts" 
           class="toast" 
           [class.success]="toast.type === 'success'"
           [class.error]="toast.type === 'error'"
           [class.info]="toast.type === 'info'">
        <div class="toast-icon">
          <svg *ngIf="toast.type === 'success'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 6L9 17l-5-5"/></svg>
          <svg *ngIf="toast.type === 'error'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          <svg *ngIf="toast.type === 'info'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        </div>
        <div class="toast-message">{{ toast.message }}</div>
        <button *ngIf="toast.action" class="toast-action" (click)="handleAction(toast)">
          {{ toast.actionLabel }}
        </button>
        <button class="toast-close" (click)="remove(toast.id)">✕</button>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 24px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 12px;
      pointer-events: none;
    }

    .toast {
      pointer-events: auto;
      min-width: 300px;
      max-width: 450px;
      background: var(--bg-2);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
      animation: slideIn 0.3s cubic-bezier(0, 0, 0.2, 1);
      transition: all 0.2s;
    }

    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }

    .toast.success { border-left: 4px solid #10b981; }
    .toast.error { border-left: 4px solid #f43f5e; }
    .toast.info { border-left: 4px solid #3b82f6; }

    .toast-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .toast.success .toast-icon { color: #10b981; }
    .toast.error .toast-icon { color: #f43f5e; }
    .toast.info .toast-icon { color: #3b82f6; }

    .toast-message {
      flex-grow: 1;
      font-size: 14px;
      font-weight: 500;
      color: var(--text);
    }

    .toast-close {
      background: none;
      border: none;
      color: var(--text-2);
      cursor: pointer;
      font-size: 16px;
      padding: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 6px;
      transition: background 0.2s;
    }

    .toast-close:hover {
      background: var(--bg);
      color: var(--text);
    }

    .toast-action {
      background: none;
      border: 1px solid var(--primary);
      color: var(--primary);
      padding: 4px 12px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      white-space: nowrap;
    }

    .toast-action:hover {
      background: var(--primary);
      color: white;
    }
  `]
})
export class ToastComponent implements OnInit {
  toasts: Toast[] = [];

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.toastService.notifications$.subscribe(toasts => {
      this.toasts = toasts;
    });
  }

  remove(id: number): void {
    this.toastService.remove(id);
  }

  handleAction(toast: Toast): void {
    if (toast.action) {
      toast.action();
      // Remove onRemove callback before removing to avoid triggering the actual deletion
      toast.onRemove = undefined;
      this.remove(toast.id);
    }
  }
}
