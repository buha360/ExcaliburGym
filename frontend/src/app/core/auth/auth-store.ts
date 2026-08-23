import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthenticationService } from '../../api/generated/api/authentication.service';
import { SetupService } from '../../api/generated/api/setup.service';
import { AuthenticatedEmployee } from '../../api/generated/model/authenticated-employee';
import { LoginOption } from '../../api/generated/model/login-option';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly authenticationApi = inject(AuthenticationService);
  private readonly setupApi = inject(SetupService);

  readonly initialized = signal(false);
  readonly setupRequired = signal(false);
  readonly currentEmployee = signal<AuthenticatedEmployee | null>(null);
  readonly loginOptions = signal<readonly LoginOption[]>([]);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly isAdmin = computed(() => this.currentEmployee()?.role === 'ADMIN');

  async initialize(): Promise<void> {
    try {
      await firstValueFrom(this.authenticationApi.getCsrfToken());
      const setup = await firstValueFrom(this.setupApi.getSetupStatus());
      this.setupRequired.set(setup.setupRequired);
      if (!setup.setupRequired) {
        const session = await firstValueFrom(this.authenticationApi.getSession());
        this.currentEmployee.set(session.authenticated ? session.employee ?? null : null);
        if (!session.authenticated) {
          await this.refreshLoginOptions();
        }
      }
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error, 'A backend jelenleg nem érhető el.'));
    } finally {
      this.initialized.set(true);
    }
  }

  async createInitialAdmin(displayName: string, pin: string): Promise<boolean> {
    return this.execute(async () => {
      const employee = await firstValueFrom(this.setupApi.createInitialAdmin({ displayName, pin }));
      this.currentEmployee.set(employee);
      this.setupRequired.set(false);
      await firstValueFrom(this.authenticationApi.getCsrfToken());
    });
  }

  async login(employeeId: string, pin: string): Promise<boolean> {
    return this.execute(async () => {
      const employee = await firstValueFrom(this.authenticationApi.login({ employeeId, pin }));
      this.currentEmployee.set(employee);
      await firstValueFrom(this.authenticationApi.getCsrfToken());
    });
  }

  async logout(): Promise<boolean> {
    return this.execute(async () => {
      await firstValueFrom(this.authenticationApi.logout());
      this.currentEmployee.set(null);
      await firstValueFrom(this.authenticationApi.getCsrfToken());
      await this.refreshLoginOptions();
    });
  }

  async refreshLoginOptions(): Promise<void> {
    const options = await firstValueFrom(this.authenticationApi.listLoginOptions());
    this.loginOptions.set(options);
  }

  clearError(): void {
    this.error.set(null);
  }

  private async execute(action: () => Promise<void>): Promise<boolean> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
      return true;
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error, 'A művelet nem sikerült.'));
      return false;
    } finally {
      this.busy.set(false);
    }
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && this.hasMessage(error.error)) {
      return error.error.message;
    }
    return fallback;
  }

  private hasMessage(value: unknown): value is { message: string } {
    return typeof value === 'object'
      && value !== null
      && 'message' in value
      && typeof value.message === 'string';
  }
}
