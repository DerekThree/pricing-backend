package com.pricing.backend.simulator;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class SimulatorService {

	private final Clock systemClock;
	private volatile Clock applicationClock;

	public SimulatorService(Clock systemClock) {
		this.systemClock = systemClock;
		this.applicationClock = systemClock;
	}

	public LocalDate getCurrentDate() {
		return LocalDate.now(applicationClock);
	}

	public LocalDate setCurrentDate(LocalDate currentDate) {
		applicationClock = Clock.fixed(currentDate.atStartOfDay(systemClock.getZone()).toInstant(), systemClock.getZone());
		return getCurrentDate();
	}
}
