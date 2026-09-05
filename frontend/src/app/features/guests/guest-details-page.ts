import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { GuestsService } from '../../api/generated/api/guests.service';
import { ProductsService } from '../../api/generated/api/products.service';
import { GuestDetails } from '../../api/generated/model/guest-details';
import { GuestPassStatusEnum } from '../../api/generated/model/guest-pass';
import { PaymentMethod } from '../../api/generated/model/payment-method';
import { PassSalePaymentMethod } from '../../api/generated/model/pass-sale-payment-method';
import { ProductDefinition } from '../../api/generated/model/product-definition';

@Component({
  selector: 'app-guest-details-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './guest-details-page.html',
  styleUrl: './guest-details-page.css',
})
export class GuestDetailsPage implements OnInit {
  private readonly guestsApi = inject(GuestsService);
  private readonly productsApi = inject(ProductsService);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  private readonly guestId = this.route.snapshot.paramMap.get('guestId') ?? '';

  protected readonly paymentMethods = PassSalePaymentMethod;
  protected readonly guest = signal<GuestDetails | null>(null);
  protected readonly products = signal<readonly ProductDefinition[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly checkInBusy = signal(false);
  protected readonly repeatConfirmation = signal(false);
  protected readonly productMenuOpen = signal(false);
  protected readonly datePickerOpen = signal(false);
  protected readonly reversalVisitId = signal<string | null>(null);
  protected readonly reversalBusy = signal(false);
  protected readonly calendarMonth = signal(this.startOfMonth(new Date()));
  protected readonly weekDays = ['H', 'K', 'Sze', 'Cs', 'P', 'Szo', 'V'];

  protected readonly passForm = this.formBuilder.nonNullable.group({
    productId: ['', Validators.required],
    validFrom: [this.localToday(), Validators.required],
    paymentMethod: [PassSalePaymentMethod.Cash, Validators.required],
  });
  protected readonly reversalForm = this.formBuilder.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(500)]],
  });
  protected readonly calendarDays = computed(() => {
    const month = this.calendarMonth();
    const firstDayOffset = (month.getDay() + 6) % 7;
    const gridStart = new Date(month.getFullYear(), month.getMonth(), 1 - firstDayOffset);
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + index);
      return { date, inCurrentMonth: date.getMonth() === month.getMonth() };
    });
  });

  ngOnInit(): void {
    void this.loadPage();
  }

  protected async sellPass(): Promise<void> {
    if (this.passForm.invalid) {
      this.passForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      const pass = await firstValueFrom(this.guestsApi.sellGuestPass(this.guestId, this.passForm.getRawValue()));
      this.success.set(`${pass.productName} sikeresen kiállítva.`);
      this.passForm.patchValue({ productId: '', validFrom: this.localToday(), paymentMethod: PassSalePaymentMethod.Cash });
      await this.loadGuest(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected selectProduct(product: ProductDefinition): void {
    this.passForm.controls.productId.setValue(product.id);
    this.productMenuOpen.set(false);
  }

  protected selectedProduct(): ProductDefinition | null {
    return this.products().find((product) => product.id === this.passForm.controls.productId.value) ?? null;
  }

  protected toggleProductMenu(): void {
    this.productMenuOpen.update((open) => !open);
    this.datePickerOpen.set(false);
  }

  protected toggleDatePicker(): void {
    if (!this.datePickerOpen()) {
      this.calendarMonth.set(this.startOfMonth(this.parseLocalDate(this.passForm.controls.validFrom.value)));
    }
    this.datePickerOpen.update((open) => !open);
    this.productMenuOpen.set(false);
  }

  protected changeCalendarMonth(offset: number): void {
    const month = this.calendarMonth();
    this.calendarMonth.set(new Date(month.getFullYear(), month.getMonth() + offset, 1));
  }

  protected selectDate(date: Date): void {
    this.passForm.controls.validFrom.setValue(this.toLocalDate(date));
    this.calendarMonth.set(this.startOfMonth(date));
    this.datePickerOpen.set(false);
  }

  protected isSelectedDate(date: Date): boolean {
    return this.toLocalDate(date) === this.passForm.controls.validFrom.value;
  }

  protected calendarMonthLabel(): string {
    return new Intl.DateTimeFormat('hu-HU', { year: 'numeric', month: 'long' }).format(this.calendarMonth());
  }

  protected selectedDateLabel(): string {
    return new Intl.DateTimeFormat('hu-HU', { year: 'numeric', month: 'short', day: 'numeric' }).format(this.parseLocalDate(this.passForm.controls.validFrom.value));
  }

  protected async checkIn(confirmRepeatedToday = false): Promise<void> {
    this.checkInBusy.set(true);
    this.repeatConfirmation.set(false);
    this.error.set(null);
    this.success.set(null);
    try {
      await firstValueFrom(this.guestsApi.checkInGuest(this.guestId, { confirmRepeatedToday }));
      this.success.set(`${this.guest()?.fullName ?? 'A vendég'} látogatása rögzítve.`);
      await this.loadGuest(false);
    } catch (error: unknown) {
      if (this.apiErrorCode(error) === 'REPEATED_SAME_DAY_CHECK_IN') {
        this.repeatConfirmation.set(true);
      } else {
        this.error.set(this.errorMessage(error));
      }
    } finally {
      this.checkInBusy.set(false);
    }
  }

  protected startReversal(checkInId: string): void {
    this.reversalVisitId.set(checkInId);
    this.reversalForm.reset({ reason: '' });
    this.error.set(null);
    this.success.set(null);
  }

  protected cancelReversal(): void {
    this.reversalVisitId.set(null);
    this.reversalForm.reset({ reason: '' });
  }

  protected async reverseVisit(checkInId: string): Promise<void> {
    if (this.reversalForm.invalid) {
      this.reversalForm.markAllAsTouched();
      return;
    }
    this.reversalBusy.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      await firstValueFrom(this.guestsApi.reverseGuestCheckIn(
        this.guestId,
        checkInId,
        this.reversalForm.getRawValue(),
      ));
      this.success.set('A téves beléptetés visszavonva; a felhasznált alkalom visszaállt.');
      this.cancelReversal();
      await this.loadGuest(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.reversalBusy.set(false);
    }
  }

  protected isToday(value: string): boolean {
    return this.gymDateKey(new Date(value)) === this.gymDateKey(new Date());
  }

  protected money(amount: number): string {
    return `${new Intl.NumberFormat('hu-HU').format(amount)} Ft`;
  }

  protected date(value: string): string {
    return new Intl.DateTimeFormat('hu-HU').format(new Date(`${value.substring(0, 10)}T00:00:00`));
  }

  protected dateTime(value: string): string {
    return new Intl.DateTimeFormat('hu-HU', {
      timeZone: 'Europe/Budapest',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  protected paymentLabel(method: PaymentMethod): string {
    return method === PaymentMethod.Cash ? 'Készpénz' : method === PaymentMethod.BankCard ? 'Bankkártya' : 'Vendégegyenleg';
  }

  protected statusLabel(status: GuestPassStatusEnum): string {
    const labels: Record<GuestPassStatusEnum, string> = {
      [GuestPassStatusEnum.Active]: 'Aktív',
      [GuestPassStatusEnum.Pending]: 'Később indul',
      [GuestPassStatusEnum.Expired]: 'Lejárt',
      [GuestPassStatusEnum.Invalidated]: 'Érvénytelenített',
    };
    return labels[status];
  }

  private async loadPage(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [guest, products] = await Promise.all([
        firstValueFrom(this.guestsApi.getGuest(this.guestId)),
        firstValueFrom(this.productsApi.listGymPassProducts()),
      ]);
      this.guest.set(guest);
      this.products.set(products);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async loadGuest(showLoader = true): Promise<void> {
    if (showLoader) this.loading.set(true);
    try {
      this.guest.set(await firstValueFrom(this.guestsApi.getGuest(this.guestId)));
    } finally {
      this.loading.set(false);
    }
  }

  private localToday(): string {
    const today = new Date();
    const offset = today.getTimezoneOffset() * 60_000;
    return new Date(today.getTime() - offset).toISOString().substring(0, 10);
  }

  private parseLocalDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  private toLocalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private startOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), 1);
  }

  private gymDateKey(date: Date): string {
    const parts = new Intl.DateTimeFormat('en-GB', {
      timeZone: 'Europe/Budapest',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(date);
    const value = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? '';
    return `${value('year')}-${value('month')}-${value('day')}`;
  }

  private apiErrorCode(error: unknown): string | null {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null && 'code' in error.error) {
      return String(error.error.code);
    }
    return null;
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse
      && typeof error.error === 'object'
      && error.error !== null
      && 'message' in error.error
      && typeof error.error.message === 'string') {
      return error.error.message;
    }
    return 'A vendégadatok művelete nem sikerült.';
  }
}
