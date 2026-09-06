import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { SystemService } from '../../api/generated/api/system.service';
import { PlatformHealth } from '../../api/generated/model/platform-health';
import { ServiceHealth } from '../../api/generated/model/service-health';

@Component({
  selector: 'app-system-health-page',
  imports: [DatePipe],
  templateUrl: './system-health-page.html',
  styleUrl: './system-health-page.css',
})
export class SystemHealthPage implements OnInit, OnDestroy {
  private readonly systemApi = inject(SystemService);
  private refreshTimer: ReturnType<typeof setInterval> | undefined;

  protected readonly health = signal<PlatformHealth | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.load();
    this.refreshTimer = setInterval(() => void this.load(false), 30_000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer !== undefined) {
      clearInterval(this.refreshTimer);
    }
  }

  protected async load(showLoader = true): Promise<void> {
    if (showLoader) {
      this.loading.set(true);
    }
    this.error.set(null);
    try {
      this.health.set(await firstValueFrom(this.systemApi.getPlatformHealth()));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected statusLabel(status: ServiceHealth['status']): string {
    const labels: Record<ServiceHealth['status'], string> = {
      UP: 'Elérhető',
      DOWN: 'Nem érhető el',
      NOT_REQUIRED: 'Helyi mód',
    };
    return labels[status];
  }

  protected serviceDescription(service: ServiceHealth): string {
    const descriptions: Record<ServiceHealth['id'], string> = {
      CORE: 'Hitelesítés és központi üzleti műveletek',
      REPORTING: 'Eseményvezérelt statisztikai projekciók',
      SAUNA: 'Időpontok és ütközésmentes foglalások',
      SOLARIUM: 'Percegyenlegek, vásárlások és felhasználások',
    };
    return descriptions[service.id];
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'A központi API jelenleg nem érhető el.';
    }
    return 'A szolgáltatások állapota nem tölthető be.';
  }
}
