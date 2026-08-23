import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, firstValueFrom, of, startWith, switchMap } from 'rxjs';
import { GuestsService } from '../../api/generated/api/guests.service';
import { GuestSummary } from '../../api/generated/model/guest-summary';

@Component({
  selector: 'app-dashboard-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
})
export class DashboardPage implements OnInit {
  private readonly guestsApi = inject(GuestsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly guests = signal<readonly GuestSummary[]>([]);
  protected readonly searching = signal(true);
  protected readonly searchError = signal<string | null>(null);
  protected readonly checkInBusyId = signal<string | null>(null);
  protected readonly repeatConfirmationId = signal<string | null>(null);
  protected readonly checkInMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(
      startWith(''),
      debounceTime(180),
      distinctUntilChanged(),
      switchMap((query) => {
        this.searching.set(true);
        this.searchError.set(null);
        return this.guestsApi.listGuests(query.trim() || undefined).pipe(
          catchError((error: unknown) => {
            this.searchError.set(this.errorMessage(error));
            return of([] as GuestSummary[]);
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((guests) => {
      this.guests.set(guests);
      this.searching.set(false);
    });

  }

  protected async checkIn(guest: GuestSummary, confirmRepeatedToday = false): Promise<void> {
    this.checkInBusyId.set(guest.id);
    this.repeatConfirmationId.set(null);
    this.searchError.set(null);
    this.checkInMessage.set(null);
    try {
      await firstValueFrom(this.guestsApi.checkInGuest(guest.id, { confirmRepeatedToday }));
      this.guests.update((items) => items.map((item) => item.id === guest.id ? { ...item, checkedInToday: true } : item));
      this.checkInMessage.set(`${guest.fullName} látogatása rögzítve.`);
    } catch (error: unknown) {
      if (this.apiErrorCode(error) === 'REPEATED_SAME_DAY_CHECK_IN') {
        this.repeatConfirmationId.set(guest.id);
      } else {
        this.searchError.set(this.errorMessage(error, 'A beléptetés nem sikerült.'));
      }
    } finally {
      this.checkInBusyId.set(null);
    }
  }

  private apiErrorCode(error: unknown): string | null {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null && 'code' in error.error) {
      return String(error.error.code);
    }
    return null;
  }

  private errorMessage(error: unknown, fallback = 'A vendégkeresés nem sikerült.'): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'A szerver jelenleg nem érhető el.';
    }
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null && 'message' in error.error) {
      return String(error.error.message);
    }
    return fallback;
  }
}
