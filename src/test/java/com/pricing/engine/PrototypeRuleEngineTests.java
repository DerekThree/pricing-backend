package com.pricing.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static com.pricing.engine.AccountBatch.Account;
import static com.pricing.engine.AccountBatch.FeeRequest;
import static com.pricing.engine.AccountBatchResult.AccountResult;
import static com.pricing.engine.AccountBatchResult.AccountStatus;
import static com.pricing.engine.AccountBatchResult.Decision;
import static com.pricing.engine.AccountBatchResult.FeeResult;
import static com.pricing.engine.AccountBatchResult.FeeStatus;
import static com.pricing.engine.PriceConfig.Branch;
import static com.pricing.engine.PriceConfig.FeeType;
import static com.pricing.engine.PriceConfig.Plan;
import static com.pricing.engine.PriceConfig.PlanFee;
import static com.pricing.engine.PriceConfig.Region;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PrototypeRuleEngineTests {

	@Test
	void pricesOneFlatFeeAtInclusivePlanBoundary() {
		UUID batchId = UUID.fromString("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b");
		PriceConfig config = new PriceConfig(
				Set.of("PROD0001"),
				Map.of("BRANCH001", new Branch("BRANCH001", "IL", "60459")),
				List.of(new Region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(new Plan(
						"PLAN0001",
						"PROD0001",
						"REGION001",
						LocalDate.of(2026, 8, 1),
						LocalDate.of(2026, 8, 31),
						List.of(new PlanFee(
								"FEE00001", FeeType.FLAT, new BigDecimal("7.5000"))))));
		FakePriceConfigRepository repository = new FakePriceConfigRepository(config);
		RuleEngine engine = new PrototypeRuleEngine(repository);
		AccountBatch batch = new AccountBatch(batchId, List.of(new Account(
				"ACCOUNT001",
				"PROD0001",
				"BRANCH001",
				LocalDate.of(2026, 8, 31),
				List.of(),
				List.of(new FeeRequest("REQUEST001", "FEE00001", null)))));

		AccountBatchResult result = engine.price(batch);

		assertEquals(new AccountBatchResult(batchId, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(new FeeResult(
						"REQUEST001",
						FeeStatus.OK,
						Decision.CHARGED,
						new BigDecimal("7.50"),
						null))))), result);
		assertEquals(1, repository.loadCount);
		assertEquals(Set.of(LocalDate.of(2026, 8, 31)), repository.pricingDates);
	}

	private static class FakePriceConfigRepository implements PriceConfigRepository {

		private final PriceConfig config;
		private int loadCount;
		private Set<LocalDate> pricingDates;

		private FakePriceConfigRepository(PriceConfig config) {
			this.config = config;
		}

		@Override
		public PriceConfig load(Set<LocalDate> pricingDates) {
			loadCount++;
			this.pricingDates = pricingDates;
			return config;
		}
	}
}
