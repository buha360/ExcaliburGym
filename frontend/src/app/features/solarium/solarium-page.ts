import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, firstValueFrom, of, switchMap } from 'rxjs';
import { GuestsService } from '../../api/generated/api/guests.service';
import { SolariumService } from '../../api/generated/api/solarium.service';
import { GuestSummary } from '../../api/generated/model/guest-summary';
import { SolariumAccount } from '../../api/generated/model/solarium-account';
import { SolariumPaymentMethod } from '../../api/generated/model/solarium-payment-method';
import { SolariumProduct } from '../../api/generated/model/solarium-product';
import { SolariumTransaction } from '../../api/generated/model/solarium-transaction';

@Component({
  selector: 'app-solarium-page', imports: [ReactiveFormsModule],
  templateUrl: './solarium-page.html', styleUrl: './solarium-page.css',
})
export class SolariumPage implements OnInit {
  private readonly guestsApi = inject(GuestsService);
  private readonly solariumApi = inject(SolariumService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly paymentMethods = SolariumPaymentMethod;
  protected readonly guestSearch = new FormControl('', { nonNullable: true });
  protected readonly guestResults = signal<readonly GuestSummary[]>([]);
  protected readonly selectedGuest = signal<GuestSummary | null>(null);
  protected readonly products = signal<readonly SolariumProduct[]>([]);
  protected readonly account = signal<SolariumAccount | null>(null);
  protected readonly loadingProducts = signal(true);
  protected readonly loadingAccount = signal(false);
  protected readonly searchingGuests = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly purchaseForm = this.formBuilder.nonNullable.group({
    productId: ['', Validators.required], quantity: [1, [Validators.required, Validators.min(1), Validators.max(20)]],
    paymentMethod: [SolariumPaymentMethod.Cash, Validators.required],
  });
  protected readonly usageForm = this.formBuilder.nonNullable.group({
    minutes: [10, [Validators.required, Validators.min(1), Validators.max(1000)]],
  });

  ngOnInit(): void {
    this.guestSearch.valueChanges.pipe(
      debounceTime(180), distinctUntilChanged(),
      switchMap((query) => {
        const value = query.trim();
        this.searchingGuests.set(value.length >= 2);
        if (value.length < 2 || this.selectedGuest()?.fullName === value) return of([] as GuestSummary[]);
        return this.guestsApi.listGuests(value).pipe(catchError(() => of([] as GuestSummary[])));
      }), takeUntilDestroyed(this.destroyRef),
    ).subscribe((results) => { this.guestResults.set(results); this.searchingGuests.set(false); });
    void this.loadProducts();
  }

  protected async selectGuest(guest: GuestSummary): Promise<void> {
    this.selectedGuest.set(guest); this.guestSearch.setValue(guest.fullName, { emitEvent: false });
    this.guestResults.set([]); await this.loadAccount();
  }
  protected clearGuest(): void {
    this.selectedGuest.set(null); this.account.set(null); this.guestSearch.setValue(''); this.clearFeedback();
  }
  protected async purchaseMinutes(): Promise<void> {
    const guest = this.selectedGuest();
    if (!guest || this.purchaseForm.invalid) { this.purchaseForm.markAllAsTouched(); return; }
    this.saving.set(true); this.clearFeedback();
    try {
      this.account.set(await firstValueFrom(this.solariumApi.purchaseSolariumMinutes({ guestId: guest.id, ...this.purchaseForm.getRawValue() })));
      this.success.set('A szolárium percek jóváírása megtörtént.');
    } catch (error: unknown) { this.error.set(this.errorMessage(error, 'A vásárlás nem sikerült.')); }
    finally { this.saving.set(false); }
  }
  protected async useMinutes(): Promise<void> {
    const guest = this.selectedGuest();
    if (!guest || this.usageForm.invalid) { this.usageForm.markAllAsTouched(); return; }
    this.saving.set(true); this.clearFeedback();
    try {
      this.account.set(await firstValueFrom(this.solariumApi.useSolariumMinutes({ guestId: guest.id, minutes: this.usageForm.controls.minutes.value })));
      this.success.set('A felhasznált perceket levontuk az egyenlegből.');
    } catch (error: unknown) { this.error.set(this.errorMessage(error, 'A perclevonás nem sikerült.')); }
    finally { this.saving.set(false); }
  }
  protected price(value: number): string { return `${value.toLocaleString('hu-HU')} Ft`; }
  protected occurredAt(value: string): string {
    return new Intl.DateTimeFormat('hu-HU', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Budapest' }).format(new Date(value));
  }
  protected transactionTitle(transaction: SolariumTransaction): string {
    return transaction.type === 'PURCHASE' ? (transaction.productName ?? 'Percjóváírás') : 'Szolárium használat';
  }
  private async loadProducts(): Promise<void> {
    this.loadingProducts.set(true);
    try {
      const products = await firstValueFrom(this.solariumApi.listSolariumProducts()); this.products.set(products);
      const firstAvailable = products.find((product) => product.active && product.defaultPrice > 0);
      if (firstAvailable) this.purchaseForm.controls.productId.setValue(firstAvailable.id);
    } catch (error: unknown) { this.error.set(this.errorMessage(error, 'A szolárium árlista nem tölthető be.')); }
    finally { this.loadingProducts.set(false); }
  }
  private async loadAccount(): Promise<void> {
    const guest = this.selectedGuest(); if (!guest) return;
    this.loadingAccount.set(true); this.clearFeedback();
    try { this.account.set(await firstValueFrom(this.solariumApi.getSolariumAccount(guest.id))); }
    catch (error: unknown) { this.error.set(this.errorMessage(error, 'A vendég szolárium-egyenlege nem tölthető be.')); }
    finally { this.loadingAccount.set(false); }
  }
  private clearFeedback(): void { this.error.set(null); this.success.set(null); }
  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null && 'message' in error.error && typeof error.error.message === 'string') return error.error.message;
    if (error instanceof HttpErrorResponse && error.status === 503) return 'A szolárium szolgáltatás jelenleg nem érhető el.';
    return fallback;
  }
}