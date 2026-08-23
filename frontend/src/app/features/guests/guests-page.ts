import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, firstValueFrom, of, startWith, switchMap } from 'rxjs';
import { GuestsService } from '../../api/generated/api/guests.service';
import { GuestRegistrationType } from '../../api/generated/model/guest-registration-type';
import { GuestSummary } from '../../api/generated/model/guest-summary';

@Component({
  selector: 'app-guests-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './guests-page.html',
  styleUrl: './guests-page.css',
})
export class GuestsPage implements OnInit {
  private readonly guestsApi = inject(GuestsService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly registrationTypes = GuestRegistrationType;
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly guests = signal<readonly GuestSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly createForm = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(160)]],
    registrationType: [GuestRegistrationType.New, Validators.required],
  });

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(
      startWith(''),
      debounceTime(180),
      distinctUntilChanged(),
      switchMap((query) => {
        this.loading.set(true);
        this.error.set(null);
        return this.guestsApi.listGuests(query.trim() || undefined).pipe(
          catchError((error: unknown) => {
            this.error.set(this.errorMessage(error));
            return of([] as GuestSummary[]);
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((guests) => {
      this.guests.set(guests);
      this.loading.set(false);
    });
  }

  protected async createGuest(): Promise<void> {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    try {
      const guest = await firstValueFrom(this.guestsApi.createGuest(this.createForm.getRawValue()));
      await this.router.navigate(['/guests', guest.id]);
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
    return 'A vendégművelet nem sikerült.';
  }
}
