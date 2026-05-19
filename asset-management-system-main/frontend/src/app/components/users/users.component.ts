import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/models';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  loading = true;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 10;

  showModal = false;
  editMode = false;
  saving = false;
  error = '';
  deleteConfirmId: number | null = null;

  form: any = this.emptyForm();
  roles = ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_VIEWER'];

  constructor(public auth: AuthService, private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAll(this.currentPage, this.pageSize).subscribe({
      next: p => {
        this.users = p.content;
        this.totalElements = p.totalElements;
        this.totalPages = p.totalPages;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.editMode = false;
    this.showModal = true;
    this.error = '';
  }

  openEdit(user: User): void {
    this.form = { 
      ...user, 
      role: user.roles[0],
      password: '' // Don't show password
    };
    this.editMode = true;
    this.showModal = true;
    this.error = '';
  }

  closeModal(): void {
    this.showModal = false;
  }

  save(): void {
    this.saving = true;
    this.error = '';

    const payload = {
      ...this.form,
      roles: [this.form.role]
    };

    const obs = this.editMode
      ? this.userService.update(this.form.id, payload)
      : this.userService.create(payload);

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.closeModal();
        this.loadUsers();
      },
      error: err => {
        this.error = err.error?.message || 'Error saving user';
        this.saving = false;
      }
    });
  }

  toggleStatus(user: User): void {
    this.userService.toggleStatus(user.id).subscribe({
      next: () => this.loadUsers(),
      error: err => alert(err.error?.message || 'Error toggling status')
    });
  }

  resetPassword(user: User): void {
    if (!confirm(`Are you sure you want to reset the password for ${user.fullName}?`)) return;
    this.userService.resetPassword(user.id).subscribe({
      next: () => alert(`Password for ${user.fullName} has been successfully reset. A mock email has been logged to /backend/sent_emails.log.`),
      error: err => alert(err.error?.message || 'Error resetting password')
    });
  }

  confirmDelete(id: number): void {
    this.deleteConfirmId = id;
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  deleteUser(): void {
    if (!this.deleteConfirmId) return;
    this.userService.delete(this.deleteConfirmId).subscribe({
      next: () => {
        this.deleteConfirmId = null;
        this.loadUsers();
      },
      error: err => alert(err.error?.message || 'Error deleting user')
    });
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadUsers();
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  getRoleBadgeClass(roles: string[]): string {
    const role = roles[0];
    if (role === 'ROLE_ADMIN') return 'badge-purple';
    if (role === 'ROLE_MANAGER') return 'badge-blue';
    return 'badge-gray';
  }

  getRoleLabel(roles: string[]): string {
    const role = roles[0];
    return role ? role.replace('ROLE_', '') : 'VIEWER';
  }

  private emptyForm() {
    return {
      fullName: '',
      username: '',
      email: '',
      password: '',
      department: '',
      employeeId: '',
      role: 'ROLE_VIEWER'
    };
  }
}
