import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthStore } from '../../core/auth/auth-store';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './login-page.html',
  styleUrls: ['./auth-page.css'],
})
export class LoginPage {
  protected readonly auth = inject(AuthStore);
  protected readonly selectedEmployeeId = signal<string | null>(null);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly form = this.formBuilder.nonNullable.group({
    pin: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
  });

  protected selectEmployee(employeeId: string): void {
    this.selectedEmployeeId.set(employeeId);
    this.auth.clearError();
  }

  protected async submit(): Promise<void> {
    const employeeId = this.selectedEmployeeId();
    if (!employeeId || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (await this.auth.login(employeeId, this.form.getRawValue().pin)) {
      await this.router.navigateByUrl('/');
    }
  }
}
