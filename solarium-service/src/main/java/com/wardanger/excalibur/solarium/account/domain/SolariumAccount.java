package com.wardanger.excalibur.solarium.account.domain;

import java.util.List;
import java.util.UUID;

public record SolariumAccount(UUID guestId, String guestName, int remainingMinutes, List<SolariumTransaction> transactions) {}