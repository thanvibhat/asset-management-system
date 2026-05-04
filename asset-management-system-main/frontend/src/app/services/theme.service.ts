import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'assettrack-theme';
  
  // Use Angular Signals for reactive state
  public theme = signal<Theme>('light');

  constructor() {
    this.initializeTheme();
  }

  private initializeTheme(): void {
    const storedTheme = localStorage.getItem(this.THEME_KEY) as Theme;
    
    if (storedTheme) {
      this.setTheme(storedTheme);
    } else {
      // Check system preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.setTheme(prefersDark ? 'dark' : 'light');
    }
  }

  public setTheme(newTheme: Theme): void {
    this.theme.set(newTheme);
    localStorage.setItem(this.THEME_KEY, newTheme);
    
    if (newTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      document.body.setAttribute('data-theme', 'dark');
    } else {
      document.documentElement.removeAttribute('data-theme');
      document.body.removeAttribute('data-theme');
    }
  }

  public toggleTheme(): void {
    this.setTheme(this.theme() === 'light' ? 'dark' : 'light');
  }
}
