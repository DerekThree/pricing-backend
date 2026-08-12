package com.pricing.backend;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PricingPlanPostgresMigrationTests {

	private static final String ACTIVE_PERIOD_OVERLAP_CONSTRAINT = "excl_pricing_plans_active_period";
	private static final String ACTIVE_PERIOD_ORDER_CONSTRAINT = "chk_pricing_plans_active_period_order";

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

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
		jdbcTemplate.update("delete from products");
		jdbcTemplate.update("delete from regions");
	}

	@Test
	void migrationEnforcesValidActivePeriodsAndPreventsConcurrentOverlaps() throws Exception {
		Long productId = insertProduct("PROD0001");
		Long regionId = insertRegion("REG00001");
		CyclicBarrier barrier = new CyclicBarrier(2);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		List<Future<Void>> writes = List.of(
				executor.submit(() -> insertAfterBarrier(barrier, "PLAN0001", productId, regionId,
						"2026-08-20", "2026-08-30")),
				executor.submit(() -> insertAfterBarrier(barrier, "PLAN0002", productId, regionId,
						"2026-08-20", "2026-08-30")));

		try {
			long successfulWrites = writes.stream().filter(this::succeeds).count();
			assertEquals(1, successfulWrites);
			assertEquals(1, writes.size() - successfulWrites);
		} finally {
			executor.shutdown();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		}

		DataIntegrityViolationException conflict = assertThrows(DataIntegrityViolationException.class,
				() -> insertPricingPlan("PLAN0003", productId, regionId, "2026-08-30", "2026-09-01"));
		assertTrue(hasSqlState(conflict, "23P01"));
		DataIntegrityViolationException invalidPeriod = assertThrows(DataIntegrityViolationException.class,
				() -> insertPricingPlan("PLAN0007", productId, regionId, "2026-09-02", "2026-09-01"));
		assertTrue(hasSqlState(invalidPeriod, "23514"));
		insertPricingPlan("PLAN0004", productId, regionId, "2026-08-31", "2026-09-01");
		insertPricingPlan("PLAN0005", insertProduct("PROD0002"), regionId, "2026-08-20", "2026-08-30");
		insertPricingPlan("PLAN0006", productId, insertRegion("REG00002"), "2026-08-20", "2026-08-30");
		assertEquals(1, jdbcTemplate.queryForObject(
				"select count(*) from pg_extension where extname = 'btree_gist'", Integer.class));
		assertEquals(1, jdbcTemplate.queryForObject(
				"select count(*) from pg_constraint where conname = ? and contype = 'x'", Integer.class,
				ACTIVE_PERIOD_OVERLAP_CONSTRAINT));
		assertEquals(1, jdbcTemplate.queryForObject(
				"select count(*) from pg_constraint where conname = ? and contype = 'c'", Integer.class,
				ACTIVE_PERIOD_ORDER_CONSTRAINT));
	}

	private Void insertAfterBarrier(CyclicBarrier barrier, String code, Long productId, Long regionId,
			String activeFrom, String activeThrough) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			await(barrier);
			insertPricingPlan(code, productId, regionId, activeFrom, activeThrough);
		});
		return null;
	}

	private boolean succeeds(Future<Void> write) {
		try {
			write.get();
			return true;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		} catch (ExecutionException exception) {
			assertTrue(hasSqlState(exception, "23P01"), exception.getCause()::toString);
			return false;
		}
	}

	private Long insertProduct(String code) {
		return jdbcTemplate.queryForObject("""
				insert into products (product_code, product_name, product_type, updated_on, updated_by)
				values (?, ?, 'DEPOSIT', ?, 'test') returning id
				""", Long.class, code, "Product " + code, OffsetDateTime.now());
	}

	private Long insertRegion(String code) {
		return jdbcTemplate.queryForObject("""
				insert into regions (region_code, region_name, updated_on, updated_by)
				values (?, ?, ?, 'test') returning id
				""", Long.class, code, "Region " + code, OffsetDateTime.now());
	}

	private void insertPricingPlan(String code, Long productId, Long regionId, String activeFrom,
			String activeThrough) {
		jdbcTemplate.update("""
				insert into pricing_plans (plan_code, plan_name, product_id, region_id, active_from,
						active_through, updated_on, updated_by)
				values (?, ?, ?, ?, ?, ?, ?, 'test')
				""", code, "Plan " + code, productId, regionId, LocalDate.parse(activeFrom),
				LocalDate.parse(activeThrough), OffsetDateTime.now());
	}

	private boolean hasSqlState(Throwable exception, String state) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof java.sql.SQLException sqlException && state.equals(sqlException.getSQLState())) {
				return true;
			}
			cause = cause.getCause();
		}

		return false;
	}

	private void await(CyclicBarrier barrier) {
		try {
			barrier.await();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		} catch (BrokenBarrierException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
