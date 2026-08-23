import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { StatisticsService } from '../../api/generated/api/statistics.service';
import { StatisticsPeriod } from '../../api/generated/model/statistics-period';
import { StatisticsSummary } from '../../api/generated/model/statistics-summary';

interface PeriodOption {
  readonly label: string;
  readonly value: StatisticsPeriod;
}

@Component({
  selector: 'app-statistics-page',
  templateUrl: './statistics-page.html',
  styleUrl: './statistics-page.css',
})
export class StatisticsPage implements OnInit {
  private readonly statisticsApi = inject(StatisticsService);

  protected readonly periods: readonly PeriodOption[] = [
    { label: 'Napi', value: StatisticsPeriod.Day },
    { label: 'Heti', value: StatisticsPeriod.Week },
    { label: 'Havi', value: StatisticsPeriod.Month },
    { label: 'Éves', value: StatisticsPeriod.Year },
  ];
  protected readonly selectedPeriod = signal<StatisticsPeriod>(StatisticsPeriod.Day);
  protected readonly statistics = signal<StatisticsSummary | null>(null);
  protected readonly cashRevenue = computed(() => this.statistics()?.cashRevenue ?? 0);
  protected readonly bankCardRevenue = computed(() => this.statistics()?.bankCardRevenue ?? 0);
  protected readonly prepaidBalanceRevenue = computed(() => this.statistics()?.prepaidBalanceRevenue ?? 0);
  protected readonly periodFrom = computed(() => this.statistics()?.from);
  protected readonly periodTo = computed(() => this.statistics()?.to);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.selectPeriod(StatisticsPeriod.Day);
  }

  protected async selectPeriod(period: StatisticsPeriod): Promise<void> {
    this.selectedPeriod.set(period);
    this.loading.set(true);
    this.error.set(null);
    try {
      this.statistics.set(await firstValueFrom(this.statisticsApi.getStatisticsSummary(period)));
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected money(amount?: number): string {
    return `${new Intl.NumberFormat('hu-HU').format(amount ?? 0)} Ft`;
  }

  protected date(value?: string): string {
    return value ? new Intl.DateTimeFormat('hu-HU').format(new Date(value)) : '–';
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'A szerver jelenleg nem érhető el.';
    }
    return 'A statisztika betöltése nem sikerült.';
  }
}
