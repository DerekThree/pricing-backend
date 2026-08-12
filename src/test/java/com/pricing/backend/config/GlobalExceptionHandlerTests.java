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
	void leavesUnrelatedIntegrityViolationsAsConflicts() {
		SQLException cause = new SQLException("duplicate key value violates unique constraint", "23505");

		ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
				.handleDataIntegrityViolation(new DataIntegrityViolationException("constraint failed", cause));

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals("A record with the same code already exists", response.getBody().getMessage());
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
