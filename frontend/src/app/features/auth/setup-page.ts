import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthStore } from '../../core/auth/auth-store';

@Component({
  selector: 'app-setup-page',
  imports: [ReactiveFormsModule],
  templateUrl: './setup-page.html',
  styleUrls: ['./auth-page.css'],
})
export class SetupPage {
  protected readonly auth = inject(AuthStore);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly form = this.formBuilder.nonNullable.group({
    displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    pin: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
    pinAgain: ['', [Validators.required]],
  });

  protected async submit(): Promise<void> {
    this.auth.clearError();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    if (value.pin !== value.pinAgain) {
      this.form.controls.pinAgain.setErrors({ mismatch: true });
      return;
    }
    if (await this.auth.createInitialAdmin(value.displayName, value.pin)) {
      await this.router.navigateByUrl('/');
    }
  }
}
