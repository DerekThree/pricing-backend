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

	private static final UUID BATCH_ID = UUID.fromString("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b");
	private static final LocalDate AUGUST_1 = LocalDate.of(2026, 8, 1);
	private static final LocalDate AUGUST_15 = LocalDate.of(2026, 8, 15);
	private static final LocalDate AUGUST_31 = LocalDate.of(2026, 8, 31);
	private static final Branch BRANCH = new Branch("BRANCH001", "IL", "60459");

	@Test
	void pricesOneFlatFeeAtInclusiveActiveThroughBoundary() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31)));
		FakePriceConfigRepository repository = new FakePriceConfigRepository(config);
		RuleEngine engine = new PrototypeRuleEngine(repository);

		AccountBatchResult result = engine.price(batch("PROD0001", "BRANCH001", AUGUST_31));

		assertEquals(chargedResult("PLAN0001"), result);
		assertEquals(1, repository.loadCount);
		assertEquals(Set.of(AUGUST_31), repository.pricingDates);
	}

	@Test
	void returnsProductNotFoundForAnInexactProductCodeBeforeBranchResolution() {
		PriceConfig config = new PriceConfig(Set.of("PROD0001"), Map.of(), List.of(), List.of());

		AccountBatchResult result = price(config, "PROD0001 ", "BRANCH001", AUGUST_15);

		assertEquals(failedResult(AccountStatus.PRODUCT_NOT_FOUND), result);
	}

	@Test
	void returnsBranchNotFoundForAnInexactBranchCodeAfterProductResolution() {
		PriceConfig config = config(List.of(), List.of());

		AccountBatchResult result = price(config, "PROD0001", "branch001", AUGUST_15);

		assertEquals(failedResult(AccountStatus.BRANCH_NOT_FOUND), result);
	}

	@Test
	void resolvesRegionByZipCodeWhenNoDirectBranchMatches() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of(), Set.of("60459"), Set.of())),
				List.of(plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31)));

		AccountBatchResult result = price(config, "PROD0001", "BRANCH001", AUGUST_15);

		assertEquals(chargedResult("PLAN0001"), result);
	}

	@Test
	void resolvesRegionByStateWhenNoBranchOrZipCodeMatches() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of(), Set.of(), Set.of("IL"))),
				List.of(plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31)));

		AccountBatchResult result = price(config, "PROD0001", "BRANCH001", AUGUST_15);

		assertEquals(chargedResult("PLAN0001"), result);
	}

	@Test
	void stopsAfterTheFirstRegionTierContainingAMatch() {
		PriceConfig config = config(
				List.of(
						region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of()),
						region("REGION002", Set.of(), Set.of("60459"), Set.of()),
						region("REGION003", Set.of(), Set.of("60459"), Set.of("IL"))),
				List.of(plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31)));

		AccountBatchResult result = price(config, "PROD0001", "BRANCH001", AUGUST_15);

		assertEquals(chargedResult("PLAN0001"), result);
	}

	@Test
	void returnsRegionNotFoundWhenNoRegionTierMatches() {
		AccountBatchResult result = price(config(List.of(), List.of()),
				"PROD0001", "BRANCH001", AUGUST_15);

		assertEquals(failedResult(AccountStatus.REGION_NOT_FOUND), result);
	}

	@Test
	void returnsErrorWhenTheWinningRegionTierIsAmbiguous() {
		PriceConfig config = config(
				List.of(
						region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of()),
						region("REGION002", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of());

		AccountBatchResult result = price(config, "PROD0001", "BRANCH001", AUGUST_15);

		assertEquals(failedResult(AccountStatus.ERROR), result);
	}

	@Test
	void selectsPlanByExactProductRegionAndInclusiveActiveFromBoundary() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(
						plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31),
						plan("PLAN0002", "PROD0002", "REGION001", AUGUST_1, AUGUST_31),
						plan("PLAN0003", "PROD0001", "REGION002", AUGUST_1, AUGUST_31),
						plan("PLAN0004", "PROD0001", "REGION001", AUGUST_15, AUGUST_31)));

		AccountBatchResult result = price(config, "PROD0001", "BRANCH001", AUGUST_1);

		assertEquals(chargedResult("PLAN0001"), result);
	}

	@Test
	void returnsPlanNotFoundWhenNoPlanMatchesProductRegionAndPricingDate() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31)));

		AccountBatchResult result = price(config, "PROD0001", "BRANCH001",
				LocalDate.of(2026, 9, 1));

		assertEquals(failedResult(AccountStatus.PLAN_NOT_FOUND), result);
	}

	@Test
	void returnsErrorWhenMultiplePricingPlansApply() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(
						plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31),
						plan("PLAN0002", "PROD0001", "REGION001", AUGUST_15,
								LocalDate.of(2026, 9, 15))));

		AccountBatchResult result = price(config, "PROD0001", "BRANCH001", AUGUST_15);

		assertEquals(failedResult(AccountStatus.ERROR), result);
	}

	private AccountBatchResult price(
			PriceConfig config,
			String productCode,
			String branchCode,
			LocalDate pricingDate) {
		return new PrototypeRuleEngine(new FakePriceConfigRepository(config))
				.price(batch(productCode, branchCode, pricingDate));
	}

	private AccountBatch batch(String productCode, String branchCode, LocalDate pricingDate) {
		return new AccountBatch(BATCH_ID, List.of(new Account(
				"ACCOUNT001",
				productCode,
				branchCode,
				pricingDate,
				List.of(),
				List.of(new FeeRequest(1L, "FEE00001", null)))));
	}

	private PriceConfig config(List<Region> regions, List<Plan> plans) {
		return new PriceConfig(
				Set.of("PROD0001"),
				Map.of("BRANCH001", BRANCH),
				regions,
				plans);
	}

	private Region region(
			String code,
			Set<String> branchCodes,
			Set<String> zipCodes,
			Set<String> states) {
		return new Region(code, branchCodes, zipCodes, states);
	}

	private Plan plan(
			String code,
			String productCode,
			String regionCode,
			LocalDate activeFrom,
			LocalDate activeThrough) {
		return new Plan(
				code,
				productCode,
				regionCode,
				activeFrom,
				activeThrough,
				List.of(new PlanFee("FEE00001", FeeType.FLAT, new BigDecimal("7.5000"))));
	}

	private AccountBatchResult chargedResult(String planCode) {
		return new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				planCode,
				List.of(new FeeResult(
						1L,
						FeeStatus.OK,
						Decision.CHARGED,
						new BigDecimal("7.50"),
						null)))));
	}

	private AccountBatchResult failedResult(AccountStatus status) {
		return new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001", status, null, null)));
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
