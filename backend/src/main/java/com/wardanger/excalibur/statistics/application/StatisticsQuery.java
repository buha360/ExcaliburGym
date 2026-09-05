package com.wardanger.excalibur.statistics.application;

public interface StatisticsQuery {

    StatisticsService.StatisticsSnapshot current(StatisticsService.Period period);
}
