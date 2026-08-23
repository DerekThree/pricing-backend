package com.pricing.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.pricing.engine.AccountBatch.Account;
import static com.pricing.engine.AccountBatch.AccountAttribute;
import static com.pricing.engine.AccountBatch.FeeRequest;
import static com.pricing.engine.AccountBatchResult.AccountResult;
import static com.pricing.engine.AccountBatchResult.AccountStatus;
import static com.pricing.engine.AccountBatchResult.Decision;
import static com.pricing.engine.AccountBatchResult.FeeResult;
import static com.pricing.engine.AccountBatchResult.FeeStatus;
import static com.pricing.engine.PriceConfig.AttributeDefinition;
import static com.pricing.engine.PriceConfig.AttributeType;
import static com.pricing.engine.PriceConfig.Branch;
import static com.pricing.engine.PriceConfig.EligibilityCondition;
import static com.pricing.engine.PriceConfig.EligibilityReason;
import static com.pricing.engine.PriceConfig.FeeType;
import static com.pricing.engine.PriceConfig.Plan;
import static com.pricing.engine.PriceConfig.PlanFee;
import static com.pricing.engine.PriceConfig.Region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
	void returnsPlanNotFoundForAnInexactProductCode() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31)));

		AccountBatchResult result = price(config, "PROD0001 ", "BRANCH001", AUGUST_15);

		assertEquals(failedResult(AccountStatus.PLAN_NOT_FOUND), result);
	}

	@Test
	void returnsBranchNotFoundForAnInexactBranchCode() {
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

	@Test
	void returnsMissingAttributeWhenARequestedPlanFeeRequiresAnAbsentAttribute() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(fee("FEE00001",
						new AttributeDefinition("ATTR0001", AttributeType.TEXT))))));

		AccountBatchResult result = price(config, batch(List.of(),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(failedResult(AccountStatus.MISSING_ATTRIBUTE), result);
	}

	@Test
	void returnsDuplicateAttribute() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(fee("FEE00001",
						new AttributeDefinition("ATTR0001", AttributeType.TEXT))))));

		AccountBatchResult result = price(config, batch(
				List.of(
						new AccountAttribute("ATTR0001", "FIRST"),
						new AccountAttribute("ATTR0001", "SECOND")),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(failedResult(AccountStatus.DUPLICATE_ATTRIBUTE), result);
	}

	@Test
	void returnsInvalidAttributeType() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(fee("FEE00001",
						new AttributeDefinition("ATTR0001", AttributeType.TEXT))))));

		AccountBatchResult result = price(config, batch(
				List.of(new AccountAttribute("ATTR0001", true)),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(failedResult(AccountStatus.INVALID_ATTRIBUTE_TYPE), result);
	}

	@ParameterizedTest
	@MethodSource("validAttributeValues")
	void acceptsEveryConfiguredAttributeRepresentation(AttributeType type, Object value) {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(fee("FEE00001",
						new AttributeDefinition("ATTR0001", type))))));

		AccountBatchResult result = price(config, batch(
				List.of(new AccountAttribute("ATTR0001", value)),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(chargedResult("PLAN0001"), result);
	}

	@Test
	void doesNotRequireAttributesForAnUnrequestedPlanFee() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(
						fee("FEE00001", new AttributeDefinition("ATTR0001", AttributeType.TEXT)),
						flatFee("FEE00002")))));

		AccountBatchResult result = price(config, batch(
				List.of(),
				List.of(new FeeRequest(1L, "FEE00002", null))));

		assertEquals(new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(new FeeResult(
						1L,
						FeeStatus.OK,
						Decision.CHARGED,
						new BigDecimal("7.50"),
						null))))), result);
	}

	@Test
	void returnsFeeNotFoundWithoutCreatingAnAttributeRequirementForAnAbsentPlanFee() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(fee("FEE00001",
						new AttributeDefinition("ATTR0001", AttributeType.TEXT))))));

		AccountBatchResult result = price(config, batch(
				List.of(),
				List.of(new FeeRequest(1L, "FEE99999", null))));

		assertEquals(new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(new FeeResult(1L, FeeStatus.FEE_NOT_FOUND, null, null, null))))), result);
	}

	@ParameterizedTest
	@MethodSource("satisfyingTypedComparisons")
	void supportsEveryTypedEligibilityOperator(
			AttributeType type, Object accountValue, String operator, String conditionValue) {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(feeWithReasons("FEE00001", reason("REASON001",
						condition("ATTR0001", type, operator, conditionValue)))))));

		AccountBatchResult result = price(config, batch(
				List.of(new AccountAttribute("ATTR0001", accountValue)),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(waivedResult("REASON001"), result);
	}

	@Test
	void requiresEveryConditionWithinOneEligibilityReason() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(feeWithReasons("FEE00001", reason("REASON001",
						condition("ATTR0001", AttributeType.TEXT, "=", "PREMIER"),
						condition("ATTR0002", AttributeType.DECIMAL, ">", "100")))))));

		AccountBatchResult result = price(config, batch(
				List.of(
						new AccountAttribute("ATTR0001", "PREMIER"),
						new AccountAttribute("ATTR0002", new BigDecimal("50"))),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(chargedResult("PLAN0001"), result);
	}

	@Test
	void waivesForAnySatisfiedReasonAndReturnsEverySatisfiedReasonCode() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(feeWithReasons(
						"FEE00001",
						reason("TEXT_REASON",
								condition("ATTR0001", AttributeType.TEXT, "=", "PREMIER")),
						reason("AMOUNT_REASON",
								condition("ATTR0002", AttributeType.DECIMAL, ">=", "100")),
						reason("BOOLEAN_REASON",
								condition("ATTR0003", AttributeType.BOOLEAN, "=", "true")))))));

		AccountBatchResult result = price(config, batch(
				List.of(
						new AccountAttribute("ATTR0001", " premier "),
						new AccountAttribute("ATTR0002", new BigDecimal("100.00")),
						new AccountAttribute("ATTR0003", false)),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		FeeResult fee = result.accounts().getFirst().fees().getFirst();
		assertEquals(FeeStatus.OK, fee.status());
		assertEquals(Decision.WAIVED, fee.decision());
		assertEquals(null, fee.amount());
		assertEquals(Set.of("TEXT_REASON", "AMOUNT_REASON"), Set.copyOf(fee.reasons()));
	}

	@Test
	void treatsAnEligibilityReasonWithoutConditionsAsSatisfied() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(feeWithReasons("FEE00001", reason("ALWAYS"))))));

		AccountBatchResult result = price(config, batch(List.of(),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(waivedResult("ALWAYS"), result);
	}

	@Test
	void returnsFeeErrorForAnInvalidOperatorAndTypeCombination() {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(
						feeWithReasons("FEE00001", reason("INVALID",
								condition("ATTR0001", AttributeType.BOOLEAN, ">", "false"))),
						flatFee("FEE00002")))));

		AccountBatchResult result = price(config, batch(
				List.of(new AccountAttribute("ATTR0001", true)),
				List.of(
						new FeeRequest(1L, "FEE00001", null),
						new FeeRequest(2L, "FEE00002", null))));

		assertEquals(new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(
						new FeeResult(1L, FeeStatus.ERROR, null, null, null),
						new FeeResult(2L, FeeStatus.OK, Decision.CHARGED,
								new BigDecimal("7.50"), null))))), result);
	}

	@ParameterizedTest
	@MethodSource("invalidConditionValues")
	void returnsFeeErrorForAnInvalidPersistedConditionValue(
			AttributeType type, Object accountValue, String conditionValue) {
		PriceConfig config = config(
				List.of(region("REGION001", Set.of("BRANCH001"), Set.of(), Set.of())),
				List.of(plan(List.of(feeWithReasons("FEE00001", reason("INVALID",
						condition("ATTR0001", type, "=", conditionValue)))))));

		AccountBatchResult result = price(config, batch(
				List.of(new AccountAttribute("ATTR0001", accountValue)),
				List.of(new FeeRequest(1L, "FEE00001", null))));

		assertEquals(new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(new FeeResult(1L, FeeStatus.ERROR, null, null, null))))), result);
	}

	private AccountBatchResult price(
			PriceConfig config,
			String productCode,
			String branchCode,
			LocalDate pricingDate) {
		return new PrototypeRuleEngine(new FakePriceConfigRepository(config))
				.price(batch(productCode, branchCode, pricingDate));
	}

	private AccountBatchResult price(PriceConfig config, AccountBatch batch) {
		return new PrototypeRuleEngine(new FakePriceConfigRepository(config)).price(batch);
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

	private AccountBatch batch(List<AccountAttribute> attributes, List<FeeRequest> fees) {
		return new AccountBatch(BATCH_ID, List.of(new Account(
				"ACCOUNT001",
				"PROD0001",
				"BRANCH001",
				AUGUST_15,
				attributes,
				fees)));
	}

	private PriceConfig config(List<Region> regions, List<Plan> plans) {
		return new PriceConfig(
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
				List.of(new PlanFee(
						"FEE00001",
						FeeType.FLAT,
						new BigDecimal("7.5000"),
						List.of())));
	}

	private Plan plan(List<PlanFee> fees) {
		return new Plan("PLAN0001", "PROD0001", "REGION001", AUGUST_1, AUGUST_31, fees);
	}

	private PlanFee fee(String code, AttributeDefinition... attributes) {
		return feeWithReasons(code, new EligibilityReason(
				"REASON001",
				Stream.of(attributes)
						.map(attribute -> new EligibilityCondition(
								attribute,
								"=",
								unmatchedValue(attribute.type())))
						.toList()));
	}

	private PlanFee flatFee(String code) {
		return new PlanFee(
				code,
				FeeType.FLAT,
				new BigDecimal("7.5000"),
				List.of());
	}

	private PlanFee feeWithReasons(String code, EligibilityReason... reasons) {
		return new PlanFee(
				code,
				FeeType.FLAT,
				new BigDecimal("7.5000"),
				List.of(reasons));
	}

	private EligibilityReason reason(String code, EligibilityCondition... conditions) {
		return new EligibilityReason(code, List.of(conditions));
	}

	private EligibilityCondition condition(
			String code, AttributeType type, String operator, String value) {
		return new EligibilityCondition(new AttributeDefinition(code, type), operator, value);
	}

	private String unmatchedValue(AttributeType type) {
		return switch (type) {
			case TEXT -> "DIFFERENT";
			case BOOLEAN -> "false";
			case DECIMAL, INTEGER -> "0";
			case DATE -> "2025-01-01";
		};
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

	private AccountBatchResult waivedResult(String... reasons) {
		return new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(new FeeResult(
						1L,
						FeeStatus.OK,
						Decision.WAIVED,
						null,
						List.of(reasons))))));
	}

	private AccountBatchResult failedResult(AccountStatus status) {
		return new AccountBatchResult(BATCH_ID, List.of(new AccountResult(
				"ACCOUNT001", status, null, null)));
	}

	private static Stream<Arguments> validAttributeValues() {
		return Stream.of(
				arguments(AttributeType.TEXT, " Value "),
				arguments(AttributeType.BOOLEAN, true),
				arguments(AttributeType.DECIMAL, new BigDecimal("1.50")),
				arguments(AttributeType.INTEGER, new BigDecimal("1.00")),
				arguments(AttributeType.DATE, "2026-08-15"));
	}

	private static Stream<Arguments> satisfyingTypedComparisons() {
		return Stream.of(
				arguments(AttributeType.TEXT, " Premium ", "=", "premium"),
				arguments(AttributeType.TEXT, "STANDARD", "<>", "premium"),
				arguments(AttributeType.BOOLEAN, true, "=", "true"),
				arguments(AttributeType.BOOLEAN, false, "<>", "true"),
				arguments(AttributeType.DECIMAL, new BigDecimal("1.0"), "=", "1.00"),
				arguments(AttributeType.DECIMAL, new BigDecimal("2"), "<>", "1"),
				arguments(AttributeType.DECIMAL, new BigDecimal("2"), ">", "1"),
				arguments(AttributeType.DECIMAL, new BigDecimal("1"), "<", "2"),
				arguments(AttributeType.DECIMAL, new BigDecimal("1"), ">=", "1.00"),
				arguments(AttributeType.DECIMAL, new BigDecimal("1"), "<=", "1.0"),
				arguments(AttributeType.INTEGER, new BigDecimal("1"), "=", "1.00"),
				arguments(AttributeType.INTEGER, new BigDecimal("2"), "<>", "1"),
				arguments(AttributeType.INTEGER, new BigDecimal("2"), ">", "1"),
				arguments(AttributeType.INTEGER, new BigDecimal("1"), "<", "2"),
				arguments(AttributeType.INTEGER, new BigDecimal("1"), ">=", "1"),
				arguments(AttributeType.INTEGER, new BigDecimal("1"), "<=", "1"),
				arguments(AttributeType.DATE, "2026-08-15", "=", "2026-08-15"),
				arguments(AttributeType.DATE, "2026-08-16", "<>", "2026-08-15"),
				arguments(AttributeType.DATE, "2026-08-16", ">", "2026-08-15"),
				arguments(AttributeType.DATE, "2026-08-14", "<", "2026-08-15"),
				arguments(AttributeType.DATE, "2026-08-15", ">=", "2026-08-15"),
				arguments(AttributeType.DATE, "2026-08-15", "<=", "2026-08-15"));
	}

	private static Stream<Arguments> invalidConditionValues() {
		return Stream.of(
				arguments(AttributeType.BOOLEAN, true, "not-a-boolean"),
				arguments(AttributeType.DECIMAL, new BigDecimal("1"), "not-a-number"),
				arguments(AttributeType.INTEGER, new BigDecimal("1"), "1.5"),
				arguments(AttributeType.DATE, "2026-08-15", "not-a-date"));
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
