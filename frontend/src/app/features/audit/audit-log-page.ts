import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuditService } from '../../api/generated/api/audit.service';
import { AuditLogEntry } from '../../api/generated/model/audit-log-entry';
import { AuthStore } from '../../core/auth/auth-store';

@Component({
  selector: 'app-audit-log-page',
  imports: [DatePipe],
  templateUrl: './audit-log-page.html',
  styleUrl: './audit-log-page.css',
})
export class AuditLogPage implements OnInit {
  private readonly auditApi = inject(AuditService);
  protected readonly auth = inject(AuthStore);
  protected readonly entries = signal<readonly AuditLogEntry[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly title = computed(() => this.auth.isAdmin() ? 'Admin napló' : 'Saját tevékenység');
  protected readonly description = computed(() => this.auth.isAdmin()
    ? 'A rendszerben történt legutóbbi dolgozói műveletek.'
    : 'A saját fiókoddal rögzített legutóbbi műveletek.');

  ngOnInit(): void {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.entries.set(await firstValueFrom(this.auditApi.listAuditLogs(200)));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected actionLabel(action: string): string {
    const labels: Readonly<Record<string, string>> = {
      ADMIN_CREATED: 'Admin létrehozása',
      EMPLOYEE_CREATED: 'Dolgozó létrehozása',
      EMPLOYEE_ACTIVATED: 'Dolgozó aktiválása',
      EMPLOYEE_DEACTIVATED: 'Dolgozó deaktiválása',
      EMPLOYEE_PIN_CHANGED: 'PIN módosítása',
      GUEST_CREATED: 'Vendég felvétele',
      PASS_SOLD: 'Bérlet kiállítása',
      GUEST_CHECKED_IN: 'Beléptetés',
      CHECK_IN_REVERSED: 'Visszavonás',
      PRODUCT_CREATED: 'Bérlettípus létrehozása',
      PRODUCT_UPDATED: 'Bérlettípus módosítása',
      PRODUCT_DEACTIVATED: 'Bérlettípus inaktiválása',
      LOGIN: 'Bejelentkezés',
      LOGOUT: 'Kijelentkezés',
    };
    return labels[action] ?? action;
  }

  protected isCorrection(action: string): boolean {
    return action.includes('REVERSED') || action.includes('DEACTIVATED');
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'A szerver jelenleg nem érhető el.';
    }
    return 'A tevékenységnapló betöltése nem sikerült.';
  }
}
