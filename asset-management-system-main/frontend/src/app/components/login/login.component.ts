import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {
    if (this.auth.isLoggedIn) this.router.navigate(['/dashboard']);
  }

  login(): void {
    if (!this.username || !this.password) { this.error = 'Please enter credentials.'; return; }
    this.loading = true;
    this.error = '';
    this.auth.login(this.username, this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error = err.error?.message || 'Invalid username or password';
        this.loading = false;
      }
    });
  }
}
