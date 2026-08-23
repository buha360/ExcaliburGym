import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ProductsService } from '../../api/generated/api/products.service';
import { ProductDefinition } from '../../api/generated/model/product-definition';
import { UpsertGymPassProductRequest } from '../../api/generated/model/upsert-gym-pass-product-request';

@Component({
  selector: 'app-product-admin-page',
  imports: [ReactiveFormsModule],
  templateUrl: './product-admin-page.html',
  styleUrl: './product-admin-page.css',
})
export class ProductAdminPage implements OnInit {
  private readonly productsApi = inject(ProductsService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly products = signal<readonly ProductDefinition[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly editingId = signal<string | null>(null);
  protected readonly deactivateConfirmationId = signal<string | null>(null);

  protected readonly productForm = this.formBuilder.group({
    name: this.formBuilder.nonNullable.control('', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]),
    validityDays: this.formBuilder.nonNullable.control(30, [Validators.required, Validators.min(1), Validators.max(3650)]),
    entryLimit: this.formBuilder.control<number | null>(null, [Validators.min(1), Validators.max(1000)]),
    defaultPrice: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(0), Validators.max(10_000_000)]),
  });

  ngOnInit(): void {
    void this.loadProducts();
  }

  protected edit(product: ProductDefinition): void {
    if (!product.active) return;
    this.editingId.set(product.id);
    this.deactivateConfirmationId.set(null);
    this.error.set(null);
    this.success.set(null);
    this.productForm.setValue({
      name: product.name,
      validityDays: product.validityDays,
      entryLimit: product.entryLimit ?? null,
      defaultPrice: product.defaultPrice,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.resetForm();
  }

  protected async save(): Promise<void> {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    const request = this.productForm.getRawValue() as UpsertGymPassProductRequest;
    try {
      const id = this.editingId();
      if (id) {
        await firstValueFrom(this.productsApi.updateGymPassProduct(id, request));
        this.success.set('A bérlettípus módosításai elmentve.');
      } else {
        await firstValueFrom(this.productsApi.createGymPassProduct(request));
        this.success.set('Az új bérlettípus elkészült.');
      }
      this.editingId.set(null);
      this.resetForm();
      await this.loadProducts(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected requestDeactivate(productId: string): void {
    this.deactivateConfirmationId.set(productId);
  }

  protected async deactivate(product: ProductDefinition): Promise<void> {
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      await firstValueFrom(this.productsApi.deactivateGymPassProduct(product.id));
      this.success.set(`${product.name} kikerült az értékesíthető bérletek közül.`);
      this.deactivateConfirmationId.set(null);
      if (this.editingId() === product.id) this.cancelEdit();
      await this.loadProducts(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected money(amount: number): string {
    return `${new Intl.NumberFormat('hu-HU').format(amount)} Ft`;
  }

  private async loadProducts(showLoader = true): Promise<void> {
    if (showLoader) this.loading.set(true);
    try {
      this.products.set(await firstValueFrom(this.productsApi.listAllGymPassProducts()));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private resetForm(): void {
    this.productForm.reset({ name: '', validityDays: 30, entryLimit: null, defaultPrice: 0 });
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse
      && typeof error.error === 'object'
      && error.error !== null
      && 'message' in error.error) {
      return String(error.error.message);
    }
    return 'A bérlettípus művelete nem sikerült.';
  }
}
