import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { EmployeesService } from '../../api/generated/api/employees.service';
import { Employee } from '../../api/generated/model/employee';
import { AuthStore } from '../../core/auth/auth-store';

@Component({
  selector: 'app-employees-page',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './employees-page.html',
  styleUrl: './employees-page.css',
})
export class EmployeesPage implements OnInit {
  private readonly employeesApi = inject(EmployeesService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthStore);

  protected readonly employees = signal<readonly Employee[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly resetPinFor = signal<string | null>(null);

  protected readonly createForm = this.formBuilder.nonNullable.group({
    displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    pin: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
  });

  protected readonly resetPinForm = this.formBuilder.nonNullable.group({
    pin: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
  });

  ngOnInit(): void {
    void this.loadEmployees();
  }

  protected async createEmployee(): Promise<void> {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const value = this.createForm.getRawValue();
    await this.run(async () => {
      await firstValueFrom(this.employeesApi.createEmployee(value));
      this.createForm.reset();
      this.success.set(`${value.displayName} dolgozói fiókja elkészült.`);
      await this.loadEmployees(false);
      await this.auth.refreshLoginOptions();
    });
  }

  protected async toggleStatus(employee: Employee): Promise<void> {
    await this.run(async () => {
      await firstValueFrom(this.employeesApi.updateEmployeeStatus(employee.id, { enabled: !employee.enabled }));
      this.success.set(employee.enabled ? 'A dolgozó deaktiválva lett.' : 'A dolgozó újra aktív.');
      await this.loadEmployees(false);
      await this.auth.refreshLoginOptions();
    });
  }

  protected startPinReset(employeeId: string): void {
    this.resetPinFor.set(employeeId);
    this.resetPinForm.reset();
    this.error.set(null);
    this.success.set(null);
  }

  protected cancelPinReset(): void {
    this.resetPinFor.set(null);
    this.resetPinForm.reset();
  }

  protected async resetPin(employee: Employee): Promise<void> {
    if (this.resetPinForm.invalid) {
      this.resetPinForm.markAllAsTouched();
      return;
    }
    const pin = this.resetPinForm.getRawValue().pin;
    await this.run(async () => {
      await firstValueFrom(this.employeesApi.resetEmployeePin(employee.id, { pin }));
      this.success.set(`${employee.displayName} PIN-kódja megváltozott.`);
      this.cancelPinReset();
    });
  }

  private async loadEmployees(showLoader = true): Promise<void> {
    if (showLoader) {
      this.loading.set(true);
    }
    try {
      this.employees.set(await firstValueFrom(this.employeesApi.listEmployees()));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse
      && typeof error.error === 'object'
      && error.error !== null
      && 'message' in error.error
      && typeof error.error.message === 'string') {
      return error.error.message;
    }
    return 'A művelet nem sikerült.';
  }
}
