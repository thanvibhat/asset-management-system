import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { NotificationService } from '../../services/notification.service';
import { ToastService } from '../../services/toast.service';
import { Notification } from '../../models/models';
import { AiChatbotComponent } from '../ai-chatbot/ai-chatbot.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, AiChatbotComponent, FormsModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  showNotifications = false;
  private pollInterval: any;
  private poppedNotificationIds = new Set<number>();

  showChangePasswordModal = false;
  resetError = '';
  resetSuccess = false;
  submittingReset = false;
  resetForm = {
    oldPassword: '',
    newPassword: '',
    confirmNewPassword: ''
  };

  constructor(
    public auth: AuthService, 
    public themeService: ThemeService,
    private notificationService: NotificationService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn) {
      this.loadNotifications();
      this.pollInterval = setInterval(() => this.checkForNewNotifications(), 10000);
    }
  }

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe(count => this.unreadCount = count);
  }

  loadNotifications(): void {
    this.loadUnreadCount();
    this.notificationService.getNotifications(0, 5).subscribe(page => {
      this.notifications = page.content;
      if (this.poppedNotificationIds.size === 0) {
        page.content.forEach(n => this.poppedNotificationIds.add(n.id));
      }
    });
  }

  checkForNewNotifications(): void {
    this.notificationService.getUnreadCount().subscribe(count => {
      if (count !== this.unreadCount) {
        this.notificationService.getNotifications(0, 5).subscribe(page => {
          const newUnread = page.content.filter(n => !n.read && !this.poppedNotificationIds.has(n.id));
          newUnread.forEach(n => {
            this.toastService.info(n.message);
            this.poppedNotificationIds.add(n.id);
          });
          this.notifications = page.content;
          this.unreadCount = count;
        });
      }
    });
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.loadNotifications();
    }
  }

  markAsRead(n: Notification): void {
    this.notificationService.markAsRead(n.id).subscribe(() => {
      n.read = true;
      this.unreadCount = Math.max(0, this.unreadCount - 1);
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.forEach(n => n.read = true);
      this.unreadCount = 0;
    });
  }

  logout(): void { this.auth.logout(); }

  getDisplayRole(): string {
    const role = this.auth.roles[0] || '';
    return role.replace('ROLE_', '');
  }

  getRoleClass(): string {
    const role = this.auth.roles[0] || '';
    return role.toLowerCase().replace('role_', '');
  }

  openChangePassword(): void {
    this.resetForm = { oldPassword: '', newPassword: '', confirmNewPassword: '' };
    this.resetError = '';
    this.resetSuccess = false;
    this.showChangePasswordModal = true;
  }

  closeChangePassword(): void {
    this.showChangePasswordModal = false;
  }

  submitChangePassword(forced: boolean): void {
    if (this.resetForm.newPassword !== this.resetForm.confirmNewPassword) {
      this.resetError = 'New password and confirmation do not match';
      return;
    }
    if (this.resetForm.newPassword.length < 6) {
      this.resetError = 'Password must be at least 6 characters';
      return;
    }

    this.resetError = '';
    this.resetSuccess = false;
    this.submittingReset = true;

    this.auth.changePassword(this.resetForm).subscribe({
      next: () => {
        this.submittingReset = false;
        this.resetSuccess = true;
        if (forced) {
          this.auth.updateCurrentUserPasswordResetFlag(false);
          this.toastService.success('Password updated successfully!');
        } else {
          this.toastService.success('Password updated successfully!');
          setTimeout(() => {
            this.closeChangePassword();
          }, 1500);
        }
      },
      error: err => {
        this.resetError = err.error?.message || 'Error changing password';
        this.submittingReset = false;
      }
    });
  }
}
