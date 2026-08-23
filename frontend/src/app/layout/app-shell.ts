import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthStore } from '../core/auth/auth-store';

@Component({
  selector: 'app-shell',
  imports: [DatePipe, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css',
})
export class AppShell {
  protected readonly auth = inject(AuthStore);
  protected readonly menuOpen = signal(false);
  protected readonly profileOpen = signal(false);
  private readonly router = inject(Router);

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
    this.profileOpen.set(false);
  }

  protected toggleProfile(): void {
    this.profileOpen.update((open) => !open);
  }

  protected closeProfile(): void {
    this.profileOpen.set(false);
  }

  protected closeMenus(): void {
    this.menuOpen.set(false);
    this.closeProfile();
  }

  protected async logout(): Promise<void> {
    if (await this.auth.logout()) {
      this.closeMenus();
      await this.router.navigateByUrl('/login');
    }
  }
}
