import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, firstValueFrom, of, startWith, switchMap } from 'rxjs';
import { GuestsService } from '../../api/generated/api/guests.service';
import { SaunaService } from '../../api/generated/api/sauna.service';
import { GuestSummary } from '../../api/generated/model/guest-summary';
import { SaunaReservation } from '../../api/generated/model/sauna-reservation';
import { SaunaReservationStatus } from '../../api/generated/model/sauna-reservation-status';

@Component({
  selector: 'app-sauna-page',
  imports: [ReactiveFormsModule],
  templateUrl: './sauna-page.html',
  styleUrl: './sauna-page.css',
})
export class SaunaPage implements OnInit {
  private readonly guestsApi = inject(GuestsService);
  private readonly saunaApi = inject(SaunaService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statuses = SaunaReservationStatus;
  protected readonly guestSearch = new FormControl('', { nonNullable: true });
  protected readonly guestResults = signal<readonly GuestSummary[]>([]);
  protected readonly selectedGuest = signal<GuestSummary | null>(null);
  protected readonly reservations = signal<readonly SaunaReservation[]>([]);
  protected readonly loadingSchedule = signal(true);
  protected readonly searchingGuests = signal(false);
  protected readonly saving = signal(false);
  protected readonly cancellingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly durationOptions = [15, 30, 45, 60] as const;

  protected readonly reservationForm = this.formBuilder.nonNullable.group({
    date: [this.today(), Validators.required],
    time: ['10:00', Validators.required],
    partySize: [1, [Validators.required, Validators.min(1), Validators.max(3)]],
    durationMinutes: [60, [Validators.required, Validators.min(15), Validators.max(60)]],
  });

  protected readonly cancellationForm = this.formBuilder.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.guestSearch.valueChanges.pipe(
      startWith(''),
      debounceTime(180),
      distinctUntilChanged(),
      switchMap((query) => {
        const trimmed = query.trim();
        this.searchingGuests.set(trimmed.length >= 2);
        if (trimmed.length < 2 || this.selectedGuest()?.fullName === trimmed) {
          return of([] as GuestSummary[]);
        }
        return this.guestsApi.listGuests(trimmed).pipe(
          catchError(() => of([] as GuestSummary[])),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((results) => {
      this.guestResults.set(results);
      this.searchingGuests.set(false);
    });

    this.reservationForm.controls.date.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => void this.loadSchedule());

    void this.loadSchedule();
  }

  protected selectGuest(guest: GuestSummary): void {
    this.selectedGuest.set(guest);
    this.guestSearch.setValue(guest.fullName, { emitEvent: false });
    this.guestResults.set([]);
  }

  protected clearGuest(): void {
    this.selectedGuest.set(null);
    this.guestSearch.setValue('');
  }

  protected async createReservation(): Promise<void> {
    const guest = this.selectedGuest();
    if (this.reservationForm.invalid || !guest) {
      this.reservationForm.markAllAsTouched();
      if (!guest) {
        this.error.set('Válassz ki egy vendéget a találati listából.');
      }
      return;
    }
    const value = this.reservationForm.getRawValue();
    const start = new Date(`${value.date}T${value.time}:00`);
    if (Number.isNaN(start.getTime())) {
      this.error.set('Az időpont nem értelmezhető.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      await firstValueFrom(this.saunaApi.createSaunaReservation({
        guestId: guest.id,
        startAt: start.toISOString(),
        partySize: value.partySize,
        durationMinutes: value.durationMinutes,
      }));
      this.success.set(`A ${value.durationMinutes} perces szaunafoglalás elkészült.`);
      await this.loadSchedule(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected beginCancellation(reservationId: string): void {
    this.cancellingId.set(reservationId);
    this.cancellationForm.reset();
  }

  protected closeCancellation(): void {
    this.cancellingId.set(null);
    this.cancellationForm.reset();
  }

  protected async confirmCancellation(reservationId: string): Promise<void> {
    if (this.cancellationForm.invalid) {
      this.cancellationForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    try {
      await firstValueFrom(this.saunaApi.cancelSaunaReservation(
        reservationId,
        this.cancellationForm.getRawValue(),
      ));
      this.success.set('A foglalás lemondva; az előzmény megmaradt.');
      this.closeCancellation();
      await this.loadSchedule(false);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected time(value: string): string {
    return new Intl.DateTimeFormat('hu-HU', {
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Europe/Budapest',
    }).format(new Date(value));
  }

  protected longDate(): string {
    return new Intl.DateTimeFormat('hu-HU', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long',
      timeZone: 'Europe/Budapest',
    }).format(new Date(`${this.reservationForm.controls.date.value}T12:00:00`));
  }

  private async loadSchedule(showLoader = true): Promise<void> {
    if (showLoader) {
      this.loadingSchedule.set(true);
    }
    this.error.set(null);
    try {
      const schedule = await firstValueFrom(
        this.saunaApi.getSaunaSchedule(this.reservationForm.controls.date.value),
      );
      this.reservations.set(schedule.reservations);
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loadingSchedule.set(false);
    }
  }

  private today(): string {
    const parts = new Intl.DateTimeFormat('en-CA', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      timeZone: 'Europe/Budapest',
    }).formatToParts(new Date());
    const value = Object.fromEntries(parts.map((part) => [part.type, part.value]));
    return `${value['year']}-${value['month']}-${value['day']}`;
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse
      && typeof error.error === 'object'
      && error.error !== null
      && 'message' in error.error
      && typeof error.error.message === 'string') {
      return error.error.message;
    }
    if (error instanceof HttpErrorResponse && error.status === 503) {
      return 'A szaunafoglalási szolgáltatás jelenleg nem érhető el.';
    }
    return 'A szaunaművelet nem sikerült.';
  }
}
