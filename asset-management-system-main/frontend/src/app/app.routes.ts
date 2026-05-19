import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./components/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    loadComponent: () => import('./components/navbar/navbar.component').then(m => m.NavbarComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'assets', loadComponent: () => import('./components/assets/assets.component').then(m => m.AssetsComponent) },
      { path: 'allocation', loadComponent: () => import('./components/allocation/allocation.component').then(m => m.AllocationComponent) },
      { path: 'maintenance', loadComponent: () => import('./components/maintenance/maintenance.component').then(m => m.MaintenanceComponent) },
      { path: 'transfer', loadComponent: () => import('./components/transfer/transfer.component').then(m => m.TransferComponent) },
      { path: 'vendors', loadComponent: () => import('./components/vendors/vendors.component').then(m => m.VendorsComponent) },
      { path: 'reports', loadComponent: () => import('./components/reports/reports.component').then(m => m.ReportsComponent) },
      { path: 'llm-reports', loadComponent: () => import('./components/llm-reports/llm-reports.component').then(m => m.LlmReportsComponent) },
      { path: 'product-master', loadComponent: () => import('./components/product-master/product-master.component').then(m => m.ProductMasterComponent) },
      { path: 'settings', loadComponent: () => import('./components/settings/settings.component').then(m => m.SettingsComponent) },
      { path: 'users', loadComponent: () => import('./components/users/users.component').then(m => m.UsersComponent) },
      { path: 'audit', loadComponent: () => import('./components/audit-logs/audit-logs.component').then(m => m.AuditLogsComponent) },
    ]
  },
  { path: '**', redirectTo: '' }
];
