package com.pricing.backend.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.pricing.engine.AccountBatch;
import com.pricing.engine.AccountBatch.Account;
import com.pricing.engine.AccountBatch.FeeRequest;
import com.pricing.engine.AccountBatchResult;
import com.pricing.engine.AccountBatchResult.AccountResult;
import com.pricing.engine.AccountBatchResult.AccountStatus;
import com.pricing.engine.AccountBatchResult.Decision;
import com.pricing.engine.AccountBatchResult.FeeResult;
import com.pricing.engine.AccountBatchResult.FeeStatus;
import com.pricing.engine.PriceConfig;
import com.pricing.engine.PriceConfigRepository;
import com.pricing.engine.RuleEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class JpaPriceConfigRepositoryPostgresTests {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RuleEngine ruleEngine;

	@Autowired
	private PriceConfigRepository configRepository;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/postgresql");
	}

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("delete from pricing_plans");
		jdbcTemplate.update("delete from eligibility_reason_conditions");
		jdbcTemplate.update("delete from eligibility_reasons");
		jdbcTemplate.update("delete from account_attribute_product_types");
		jdbcTemplate.update("delete from account_attributes");
		jdbcTemplate.update("delete from fee_product_types");
		jdbcTemplate.update("delete from fees");
		jdbcTemplate.update("delete from region_branches");
		jdbcTemplate.update("delete from region_zip_codes");
		jdbcTemplate.update("delete from branches");
		jdbcTemplate.update("delete from regions");
		jdbcTemplate.update("delete from products");
	}

	@Test
	void pricesFlatFeeFromPostgresConfiguration() {
		UUID batchId = UUID.fromString("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b");
		Long productId = insertProduct();
		Long branchId = insertBranch();
		Long regionId = insertRegion(branchId);
		Long feeId = insertFee("FEE00001");
		Long pricingPlanId = insertPricingPlan(
				"PLAN0001",
				productId,
				regionId,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31));
		jdbcTemplate.update("insert into pricing_plan_fees values (?, ?, ?)",
				pricingPlanId, feeId, new BigDecimal("7.5000"));
		AccountBatch batch = new AccountBatch(batchId, List.of(new Account(
				"ACCOUNT001",
				"PROD0001",
				"BRANCH001",
				LocalDate.of(2026, 8, 31),
				List.of(),
				List.of(new FeeRequest(1L, "FEE00001", null)))));

		AccountBatchResult result = ruleEngine.price(batch);

		assertEquals(new AccountBatchResult(batchId, List.of(new AccountResult(
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
	void pricesChargedAndWaivedFeesFromPersistedEligibilityConditions() {
		UUID batchId = UUID.fromString("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b");
		Long productId = insertProduct();
		Long branchId = insertBranch();
		Long regionId = insertRegion(branchId);
		Long waivedFeeId = insertFee("FEE00001");
		Long chargedFeeId = insertFee("FEE00002");
		Long pricingPlanId = insertPricingPlan(
				"PLAN0001",
				productId,
				regionId,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31));
		jdbcTemplate.update("insert into pricing_plan_fees values (?, ?, ?)",
				pricingPlanId, waivedFeeId, new BigDecimal("7.5000"));
		jdbcTemplate.update("insert into pricing_plan_fees values (?, ?, ?)",
				pricingPlanId, chargedFeeId, new BigDecimal("8.5000"));
		Long attributeId = insertTextAttribute();
		Long waivedReasonId = insertReason("ELIG0001");
		Long chargedReasonId = insertReason("ELIG0002");
		insertCondition(waivedReasonId, attributeId, "PREMIER");
		insertCondition(chargedReasonId, attributeId, "STANDARD");
		jdbcTemplate.update("insert into pricing_plan_fee_reasons values (?, ?, ?)",
				pricingPlanId, waivedFeeId, waivedReasonId);
		jdbcTemplate.update("insert into pricing_plan_fee_reasons values (?, ?, ?)",
				pricingPlanId, chargedFeeId, chargedReasonId);
		AccountBatch batch = new AccountBatch(batchId, List.of(new Account(
				"ACCOUNT001",
				"PROD0001",
				"BRANCH001",
				LocalDate.of(2026, 8, 31),
				List.of(new AccountBatch.AccountAttribute("ATTR0001", " premier ")),
				List.of(
						new FeeRequest(1L, "FEE00001", null),
						new FeeRequest(2L, "FEE00002", null)))));

		AccountBatchResult result = ruleEngine.price(batch);

		assertEquals(new AccountBatchResult(batchId, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(
						new FeeResult(
								1L,
								FeeStatus.OK,
								Decision.WAIVED,
								null,
								List.of("ELIG0001")),
						new FeeResult(
								2L,
								FeeStatus.OK,
								Decision.CHARGED,
								new BigDecimal("8.50"),
								null))))), result);
	}

	@Test
	void loadsOnlyPricingPlansActiveOnSubmittedPricingDates() {
		Long productId = insertProduct();
		Long branchId = insertBranch();
		Long regionId = insertRegion(branchId);
		insertPricingPlan(
				"PLAN0001",
				productId,
				regionId,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31));
		insertPricingPlan(
				"PLAN0002",
				productId,
				regionId,
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 30));
		insertPricingPlan(
				"PLAN0003",
				productId,
				regionId,
				LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 10, 31));

		PriceConfig config = configRepository.load(Set.of(
				LocalDate.of(2026, 8, 15),
				LocalDate.of(2026, 10, 15)));

		assertEquals(Set.of("PLAN0001", "PLAN0003"), config.plans().stream()
				.map(PriceConfig.Plan::code)
				.collect(Collectors.toSet()));
	}

	@Test
	void resolvesZipCodeRegionAndReturnsPlanNotFoundForAnotherPricingDate() {
		Long productId = insertProduct();
		insertBranch();
		Long regionId = insertRegionByZipCode();
		Long feeId = insertFee("FEE00001");
		Long pricingPlanId = insertPricingPlan(
				"PLAN0001",
				productId,
				regionId,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31));
		jdbcTemplate.update("insert into pricing_plan_fees values (?, ?, ?)",
				pricingPlanId, feeId, new BigDecimal("7.5000"));
		AccountBatch batch = new AccountBatch(
				UUID.fromString("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b"),
				List.of(
						new Account(
								"ACCOUNT001",
								"PROD0001",
								"BRANCH001",
								LocalDate.of(2026, 8, 31),
								List.of(),
								List.of(new FeeRequest(1L, "FEE00001", null))),
						new Account(
								"ACCOUNT002",
								"PROD0001",
								"BRANCH001",
								LocalDate.of(2026, 9, 1),
								List.of(),
								List.of(new FeeRequest(2L, "FEE00001", null)))));

		AccountBatchResult result = ruleEngine.price(batch);

		Map<String, AccountResult> accounts = result.accounts().stream()
				.collect(Collectors.toMap(AccountResult::accountNumber, account -> account));
		assertEquals(new AccountResult(
				"ACCOUNT001",
				AccountStatus.OK,
				"PLAN0001",
				List.of(new FeeResult(
						1L,
						FeeStatus.OK,
						Decision.CHARGED,
						new BigDecimal("7.50"),
						null))), accounts.get("ACCOUNT001"));
		assertEquals(new AccountResult(
				"ACCOUNT002",
				AccountStatus.PLAN_NOT_FOUND,
				null,
				null), accounts.get("ACCOUNT002"));
	}

	private Long insertProduct() {
		return jdbcTemplate.queryForObject("""
				insert into products (product_code, product_name, product_type, updated_on, updated_by)
				values ('PROD0001', 'Premier Checking', 'DEPOSIT', ?, 'test') returning id
				""", Long.class, OffsetDateTime.now());
	}

	private Long insertBranch() {
		return jdbcTemplate.queryForObject("""
				insert into branches (branch_code, branch_name, state, zip_code, updated_on, updated_by)
				values ('BRANCH001', 'Chicago 104th St.', 'IL', '60459', ?, 'test') returning id
				""", Long.class, OffsetDateTime.now());
	}

	private Long insertRegion(Long branchId) {
		Long regionId = jdbcTemplate.queryForObject("""
				insert into regions (region_code, region_name, updated_on, updated_by)
				values ('REGION001', 'Midwest', ?, 'test') returning id
				""", Long.class, OffsetDateTime.now());
		jdbcTemplate.update("insert into region_branches (region_id, branch_id) values (?, ?)",
				regionId, branchId);
		return regionId;
	}

	private Long insertRegionByZipCode() {
		Long regionId = jdbcTemplate.queryForObject("""
				insert into regions (region_code, region_name, updated_on, updated_by)
				values ('REGION001', 'Midwest', ?, 'test') returning id
				""", Long.class, OffsetDateTime.now());
		jdbcTemplate.update("insert into region_zip_codes (region_id, zip_code) values (?, '60459')",
				regionId);
		return regionId;
	}

	private Long insertFee(String feeCode) {
		Long feeId = jdbcTemplate.queryForObject("""
				insert into fees (fee_code, fee_name, fee_type, updated_on, updated_by)
				values (?, 'Monthly Maintenance Fee', 'FLAT', ?, 'test') returning id
				""", Long.class, feeCode, OffsetDateTime.now());
		jdbcTemplate.update("insert into fee_product_types values (?, 0, 'DEPOSIT')", feeId);
		return feeId;
	}

	private Long insertTextAttribute() {
		return jdbcTemplate.queryForObject("""
				insert into account_attributes (
						attribute_code, attribute_name, attribute_type, updated_on, updated_by)
				values ('ATTR0001', 'Account Tier', 'TEXT', ?, 'test') returning id
				""", Long.class, OffsetDateTime.now());
	}

	private Long insertReason(String reasonCode) {
		return jdbcTemplate.queryForObject("""
				insert into eligibility_reasons (
						reason_code, reason_name, updated_on, updated_by)
				values (?, 'Premier Account', ?, 'test') returning id
				""", Long.class, reasonCode, OffsetDateTime.now());
	}

	private void insertCondition(Long reasonId, Long attributeId, String value) {
		jdbcTemplate.update("""
				insert into eligibility_reason_conditions (
						reason_id, attribute_id, operator, attribute_value)
				values (?, ?, '=', ?)
				""", reasonId, attributeId, value);
	}

	private Long insertPricingPlan(
			String planCode,
			Long productId,
			Long regionId,
			LocalDate activeFrom,
			LocalDate activeThrough) {
		return jdbcTemplate.queryForObject("""
				insert into pricing_plans (plan_code, plan_name, product_id, region_id, active_from,
						active_through, updated_on, updated_by)
				values (?, 'Premier Midwest', ?, ?, ?, ?, ?, 'test')
				returning id
				""", Long.class, planCode, productId, regionId, activeFrom, activeThrough,
				OffsetDateTime.now());
	}
}
