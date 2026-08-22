package com.pricing.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PriceConfig(
		Set<String> productCodes,
		Map<String, Branch> branches,
		List<Region> regions,
		List<Plan> plans) {

	public PriceConfig {
		productCodes = Set.copyOf(productCodes);
		branches = Map.copyOf(branches);
		regions = List.copyOf(regions);
		plans = List.copyOf(plans);
	}

	public record Branch(String code, String state, String zipCode) {
	}

	public record Region(
			String code,
			Set<String> branchCodes,
			Set<String> zipCodes,
			Set<String> states) {

		public Region {
			branchCodes = Set.copyOf(branchCodes);
			zipCodes = Set.copyOf(zipCodes);
			states = Set.copyOf(states);
		}
	}

	public record Plan(
			String code,
			String productCode,
			String regionCode,
			LocalDate activeFrom,
			LocalDate activeThrough,
			List<PlanFee> fees) {

		public Plan {
			fees = List.copyOf(fees);
		}
	}

	public record PlanFee(String code, FeeType type, BigDecimal amount) {
	}

	public enum FeeType {
		FLAT,
		PERCENT
	}
}
