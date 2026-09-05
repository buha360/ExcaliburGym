package com.wardanger.excalibur.solarium.account.domain;

import java.time.Instant;
import java.util.UUID;

public record SolariumProduct(UUID id, String code, String name, int minutesPerUnit, long defaultPrice, boolean active, Instant updatedAt) {}