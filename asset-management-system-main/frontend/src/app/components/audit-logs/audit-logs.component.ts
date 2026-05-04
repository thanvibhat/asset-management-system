import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditService } from '../../services/audit.service';
import { Page } from '../../models/models';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div class="page-title-group">
          <h2 class="page-title">Audit Logs</h2>
          <span class="page-subtitle">Track system activities</span>
        </div>
      </div>

      <div class="card mt-2">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>User</th>
                <th>Action</th>
                <th>Entity</th>
                <th>Entity ID</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let log of logsPage?.content">
                <td>{{ log.performedAt | date:'medium' }}</td>
                <td><span class="badge badge-info">{{ log.performedBy }}</span></td>
                <td>
                  <span class="badge" [ngClass]="{
                    'badge-success': log.action === 'CREATE',
                    'badge-warning': log.action === 'UPDATE',
                    'badge-danger': log.action === 'DELETE'
                  }">{{ log.action }}</span>
                </td>
                <td>{{ log.entityType }}</td>
                <td>{{ log.entityId }}</td>
                <td class="text-truncate" style="max-width: 300px;" [title]="log.details">
                  {{ formatDetails(log.details) }}
                </td>
              </tr>
            </tbody>
          </table>
          <div *ngIf="loading" class="spinner"></div>
        </div>
        
        <div class="pagination-footer" *ngIf="logsPage">
          <span class="pagination-info">Showing {{ logsPage.number * logsPage.size + 1 }} to {{ (logsPage.number + 1) * logsPage.size }} of {{ logsPage.totalElements }}</span>
          <div class="pagination-btns">
            <button class="btn btn-secondary btn-sm" [disabled]="logsPage.first" (click)="loadLogs(logsPage.number - 1)">Previous</button>
            <button class="btn btn-secondary btn-sm" [disabled]="logsPage.last" (click)="loadLogs(logsPage.number + 1)">Next</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 20px; }
    .page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
    .page-title-group { display: flex; align-items: baseline; gap: 10px; }
    .page-title { font-size: 22px; margin: 0; }
    .page-subtitle { color: var(--text-3); font-size: 12px; font-weight: 500; }
    .text-truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .pagination-footer { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-top: 1px solid var(--border); }
    .pagination-info { font-size: 12px; color: var(--text-3); }
    .pagination-btns { display: flex; gap: 8px; }
    .mt-2 { margin-top: 8px; }
  `]
})
export class AuditLogsComponent implements OnInit {
  logsPage: Page<any> | null = null;
  loading = true;

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadLogs(0);
  }

  loadLogs(page: number): void {
    this.loading = true;
    this.auditService.getLogs(page).subscribe({
      next: p => { this.logsPage = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  formatDetails(details: any): string {
    if (!details) return '-';
    if (typeof details === 'string') {
      try { details = JSON.parse(details); } catch { return details; }
    }
    const str = JSON.stringify(details);
    return str.length > 100 ? str.substring(0, 100) + '...' : str;
  }
}
