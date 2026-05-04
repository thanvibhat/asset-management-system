import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../models/models';
import { AiChatbotComponent } from '../ai-chatbot/ai-chatbot.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, AiChatbotComponent],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  showNotifications = false;
  private pollInterval: any;

  constructor(
    public auth: AuthService, 
    public themeService: ThemeService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn) {
      this.loadNotifications();
      this.pollInterval = setInterval(() => this.loadUnreadCount(), 60000);
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
    this.notificationService.getNotifications(0, 5).subscribe(page => this.notifications = page.content);
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
}
