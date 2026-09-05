import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { RetailService } from '../../api/generated/api/retail.service';
import { RetailProduct } from '../../api/generated/model/retail-product';

@Component({
  selector: 'app-retail-product-admin-page',
  imports: [ReactiveFormsModule],
  templateUrl: './retail-product-admin-page.html',
  styleUrl: './retail-product-admin-page.css',
})
export class RetailProductAdminPage implements OnInit {
  private readonly api = inject(RetailService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly products = signal<readonly RetailProduct[]>([]);
  protected readonly editingId = signal<string | null>(null);
  protected readonly deactivateId = signal<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    defaultPrice: [0, [Validators.required, Validators.min(0), Validators.max(10_000_000)]],
  });

  ngOnInit(): void {
    void this.load();
  }

  protected edit(product: RetailProduct): void {
    if (!product.active) return;
    this.editingId.set(product.id);
    this.deactivateId.set(null);
    this.form.setValue({ name: product.name, defaultPrice: product.defaultPrice });
    this.clearFeedback();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancel(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', defaultPrice: 0 });
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.clearFeedback();
    try {
      const id = this.editingId();
      if (id) {
        await firstValueFrom(this.api.updateRetailProduct(id, this.form.getRawValue()));
        this.success.set('A termék módosításai elmentve.');
      } else {
        await firstValueFrom(this.api.createRetailProduct(this.form.getRawValue()));
        this.success.set('Az új termék elkészült.');
      }
      this.cancel();
      await this.load(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected async deactivate(product: RetailProduct): Promise<void> {
    this.saving.set(true);
    this.clearFeedback();
    try {
      await firstValueFrom(this.api.deactivateRetailProduct(product.id));
      this.success.set(product.name + ' kikerült az értékesíthető termékek közül.');
      this.deactivateId.set(null);
      if (this.editingId() === product.id) this.cancel();
      await this.load(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected money(value: number): string {
    return new Intl.NumberFormat('hu-HU').format(value) + ' Ft';
  }

  private async load(showLoader = true): Promise<void> {
    if (showLoader) this.loading.set(true);
    try {
      this.products.set(await firstValueFrom(this.api.listAllRetailProducts()));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private clearFeedback(): void {
    this.error.set(null);
    this.success.set(null);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null
      && 'message' in error.error) return String(error.error.message);
    return 'A termékművelet nem sikerült.';
  }
}