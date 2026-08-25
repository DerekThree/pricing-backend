package com.pricing.engine;

import java.time.LocalDate;
import java.util.Set;

public interface PriceConfigRepository {

	PriceConfig load(Set<LocalDate> pricingDates);
}
