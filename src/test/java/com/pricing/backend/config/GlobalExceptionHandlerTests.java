package com.pricing.backend.config;

import java.sql.SQLException;

import com.pricing.backend.generated.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTests {

	@Test
	void mapsTheActivePeriodExclusionConstraintToBadRequest() {
		SQLException cause = new SQLException(
				"ERROR: conflicting key value violates exclusion constraint \"excl_pricing_plans_active_period\"",
				"23P01");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Pricing plan active period overlaps an existing pricing plan", response.getBody().getMessage());
	}

	@Test
	void mapsTheActivePeriodOrderCheckConstraintToBadRequest() {
		SQLException cause = new SQLException(
				"ERROR: new row violates check constraint \"chk_pricing_plans_active_period_order\"", "23514");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Active Through must be on or after Active From", response.getBody().getMessage());
	}

	@Test
	void mapsUniqueConstraintViolationsToConflicts() {
		SQLException cause = new SQLException("duplicate key value violates unique constraint", "23505");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals("A record with the same code already exists", response.getBody().getMessage());
	}

	@Test
	void mapsDuplicatePricingPlanFeesToBadRequests() {
		SQLException cause = new SQLException(
				"ERROR: duplicate key value violates unique constraint \"pk_pricing_plan_fees\"", "23505");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("A pricing plan cannot contain the same fee twice", response.getBody().getMessage());
	}

	@Test
	void mapsDuplicatePricingPlanFeeReasonsToBadRequests() {
		SQLException cause = new SQLException(
				"ERROR: duplicate key value violates unique constraint \"pk_pricing_plan_fee_reasons\"", "23505");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("A fee cannot contain the same eligibility reason twice", response.getBody().getMessage());
	}

	@Test
	void mapsDuplicateFeeProductTypesToBadRequests() {
		SQLException cause = new SQLException(
				"ERROR: duplicate key value violates unique constraint \"pk_fee_product_types\"",
				"23505");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("A fee cannot contain the same product type twice", response.getBody().getMessage());
	}

	@Test
	void mapsDuplicateAccountAttributeProductTypesToBadRequests() {
		SQLException cause = new SQLException(
				"ERROR: duplicate key value violates unique constraint "
						+ "\"pk_account_attribute_product_types\"",
				"23505");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("An account attribute cannot contain the same product type twice",
				response.getBody().getMessage());
	}

	@Test
	void mapsForeignKeyConstraintViolationsToConflicts() {
		SQLException cause = new SQLException("violates foreign key constraint", "23503");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals("The request violates a record relationship", response.getBody().getMessage());
	}

	@Test
	void mapsH2ForeignKeyConstraintViolationsToConflicts() {
		SQLException cause = new SQLException("referenced row does not exist", "23506");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals("The request violates a record relationship", response.getBody().getMessage());
	}

	@Test
	void mapsCheckConstraintViolationsToBadRequests() {
		SQLException cause = new SQLException("violates check constraint", "23514");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("The request violates a database constraint", response.getBody().getMessage());
	}

	@Test
	void mapsH2CheckConstraintViolationsToBadRequests() {
		SQLException cause = new SQLException("violates check constraint", "23513");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("The request violates a database constraint", response.getBody().getMessage());
	}

	@Test
	void mapsNotNullConstraintViolationsToBadRequests() {
		SQLException cause = new SQLException("null value violates not-null constraint", "23502");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("The request violates a database constraint", response.getBody().getMessage());
	}

	@Test
	void mapsDataExceptionsToBadRequests() {
		SQLException cause = new SQLException("value too long for type", "22001");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("The request data is invalid", response.getBody().getMessage());
	}

	@Test
	void doesNotTreatAnotherExclusionConstraintAsAnActivePeriodOverlap() {
		SQLException cause = new SQLException(
				"ERROR: conflicting key value violates exclusion constraint \"another_constraint\"", "23P01");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
	}
}
