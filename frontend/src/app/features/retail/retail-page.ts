import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, firstValueFrom, of, switchMap } from 'rxjs';
import { GuestsService } from '../../api/generated/api/guests.service';
import { RetailService } from '../../api/generated/api/retail.service';
import { BalanceTransactionType } from '../../api/generated/model/balance-transaction-type';
import { DepositPaymentMethod } from '../../api/generated/model/deposit-payment-method';
import { GuestBalanceAccount } from '../../api/generated/model/guest-balance-account';
import { GuestSummary } from '../../api/generated/model/guest-summary';
import { RetailPaymentMethod } from '../../api/generated/model/retail-payment-method';
import { RetailProduct } from '../../api/generated/model/retail-product';

interface CartLine {
  readonly product: RetailProduct;
  readonly quantity: number;
}

@Component({
  selector: 'app-retail-page',
  imports: [ReactiveFormsModule],
  templateUrl: './retail-page.html',
  styleUrl: './retail-page.css',
})
export class RetailPage implements OnInit {
  private readonly retailApi = inject(RetailService);
  private readonly guestsApi = inject(GuestsService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly paymentMethods = RetailPaymentMethod;
  protected readonly depositMethods = DepositPaymentMethod;
  protected readonly transactionTypes = BalanceTransactionType;
  protected readonly products = signal<readonly RetailProduct[]>([]);
  protected readonly cart = signal<readonly CartLine[]>([]);
  protected readonly total = computed(() => this.cart().reduce((sum, line) => sum + line.product.defaultPrice * line.quantity, 0));
  protected readonly guestSearch = new FormControl('', { nonNullable: true });
  protected readonly guestResults = signal<readonly GuestSummary[]>([]);
  protected readonly selectedGuest = signal<GuestSummary | null>(null);
  protected readonly balance = signal<GuestBalanceAccount | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly checkoutForm = this.formBuilder.nonNullable.group({
    paymentMethod: [RetailPaymentMethod.Cash, Validators.required],
  });
  protected readonly depositForm = this.formBuilder.nonNullable.group({
    amount: [5000, [Validators.required, Validators.min(1), Validators.max(10_000_000)]],
    paymentMethod: [DepositPaymentMethod.Cash, Validators.required],
  });

  ngOnInit(): void {
    this.guestSearch.valueChanges.pipe(
      debounceTime(180),
      distinctUntilChanged(),
      switchMap((query) => {
        const value = query.trim();
        if (value.length < 2 || this.selectedGuest()?.fullName === value) return of([] as GuestSummary[]);
        return this.guestsApi.listGuests(value).pipe(catchError(() => of([] as GuestSummary[])));
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((results) => this.guestResults.set(results));
    void this.loadProducts();
  }

  protected add(product: RetailProduct): void {
    if (!product.active || product.defaultPrice <= 0) return;
    this.cart.update((lines) => {
      const existing = lines.find((line) => line.product.id === product.id);
      return existing
        ? lines.map((line) => line.product.id === product.id ? { ...line, quantity: line.quantity + 1 } : line)
        : [...lines, { product, quantity: 1 }];
    });
    this.clearFeedback();
  }

  protected changeQuantity(productId: string, delta: number): void {
    this.cart.update((lines) => lines
      .map((line) => line.product.id === productId ? { ...line, quantity: line.quantity + delta } : line)
      .filter((line) => line.quantity > 0));
  }

  protected async selectGuest(guest: GuestSummary): Promise<void> {
    this.selectedGuest.set(guest);
    this.guestSearch.setValue(guest.fullName, { emitEvent: false });
    this.guestResults.set([]);
    await this.loadBalance();
  }

  protected clearGuest(): void {
    if (this.checkoutForm.controls.paymentMethod.value === RetailPaymentMethod.PrepaidBalance) {
      this.checkoutForm.controls.paymentMethod.setValue(RetailPaymentMethod.Cash);
    }
    this.selectedGuest.set(null);
    this.balance.set(null);
    this.guestSearch.setValue('');
    this.guestResults.set([]);
  }

  protected async deposit(): Promise<void> {
    const guest = this.selectedGuest();
    if (!guest || this.depositForm.invalid) {
      this.depositForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.clearFeedback();
    try {
      this.balance.set(await firstValueFrom(this.retailApi.createGuestBalanceDeposit(guest.id, this.depositForm.getRawValue())));
      this.success.set('Az egyenlegfeltöltés sikeresen megtörtént.');
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error, 'Az egyenlegfeltöltés nem sikerült.'));
    } finally {
      this.saving.set(false);
    }
  }

  protected async checkout(): Promise<void> {
    const guest = this.selectedGuest();
    const method = this.checkoutForm.controls.paymentMethod.value;
    if (!this.cart().length || (method === RetailPaymentMethod.PrepaidBalance && !guest)) return;
    this.saving.set(true);
    this.clearFeedback();
    try {
      const sale = await firstValueFrom(this.retailApi.createRetailSale({
        guestId: guest?.id,
        paymentMethod: method,
        items: this.cart().map((line) => ({ productId: line.product.id, quantity: line.quantity })),
      }));
      this.cart.set([]);
      this.success.set('A vásárlás rögzítve: ' + this.money(sale.totalPrice) + '.');
      if (guest) await this.loadBalance(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error, 'A vásárlás rögzítése nem sikerült.'));
    } finally {
      this.saving.set(false);
    }
  }

  protected money(value: number): string {
    return new Intl.NumberFormat('hu-HU').format(value) + ' Ft';
  }

  protected dateTime(value: string): string {
    return new Intl.DateTimeFormat('hu-HU', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
      timeZone: 'Europe/Budapest',
    }).format(new Date(value));
  }

  protected transactionLabel(type: BalanceTransactionType): string {
    return type === BalanceTransactionType.Deposit ? 'Egyenlegfeltöltés' : 'Vásárlás egyenlegből';
  }

  private async loadProducts(): Promise<void> {
    this.loading.set(true);
    try {
      this.products.set(await firstValueFrom(this.retailApi.listRetailProducts()));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error, 'A termékek nem tölthetők be.'));
    } finally {
      this.loading.set(false);
    }
  }

  private async loadBalance(clearFeedback = true): Promise<void> {
    const guest = this.selectedGuest();
    if (!guest) return;
    if (clearFeedback) this.clearFeedback();
    try {
      this.balance.set(await firstValueFrom(this.retailApi.getGuestBalance(guest.id)));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error, 'A vendégegyenleg nem tölthető be.'));
    }
  }

  private clearFeedback(): void {
    this.error.set(null);
    this.success.set(null);
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null
      && 'message' in error.error && typeof error.error.message === 'string') return error.error.message;
    return fallback;
  }
}