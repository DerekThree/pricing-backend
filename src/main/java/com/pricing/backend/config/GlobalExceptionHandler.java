package com.pricing.backend.config;

import java.sql.SQLException;
import java.util.Locale;
import java.util.stream.Collectors;

import com.pricing.backend.generated.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String ACTIVE_PERIOD_OVERLAP_CONSTRAINT = "excl_pricing_plans_active_period";
	private static final String ACTIVE_PERIOD_ORDER_CONSTRAINT = "chk_pricing_plans_active_period_order";
	private static final String PRICING_PLAN_FEE_PRIMARY_KEY = "pk_pricing_plan_fees";
	private static final String PRICING_PLAN_FEE_REASON_PRIMARY_KEY = "pk_pricing_plan_fee_reasons";
	private static final String FEE_PRODUCT_TYPE_UNIQUE_CONSTRAINT = "uk_fee_product_types_fee_id_product_type";
	private static final String ACCOUNT_ATTRIBUTE_PRODUCT_TYPE_UNIQUE_CONSTRAINT =
			"uk_account_attribute_product_types_attribute_id_product_type";
	private static final String UNIQUE_VIOLATION = "23505";
	private static final String FOREIGN_KEY_VIOLATION = "23503";
	private static final String H2_FOREIGN_KEY_VIOLATION = "23506";
	private static final String NOT_NULL_VIOLATION = "23502";
	private static final String CHECK_VIOLATION = "23514";
	private static final String H2_CHECK_VIOLATION = "23513";

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> (fieldError.getField().equals("productTypes")
						? "Applicable Product Types"
						: fieldError.getField()) + " " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));

		return badRequest(message.isBlank() ? "Request validation failed" : message);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		String message = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
				.collect(Collectors.joining(", "));

		return badRequest(message.isBlank() ? "Request validation failed" : message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable() {
		return badRequest("Request body is malformed");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return badRequest(ex.getName() + " must be a valid " + ex.getRequiredType().getSimpleName().toLowerCase());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex) {
		return badRequest(ex.getParameterName() + " is required");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		return badRequest(ex.getMessage());
	}

	@ExceptionHandler(RecordNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRecordNotFound(RecordNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(RecordInUseException.class)
	public ResponseEntity<ErrorResponse> handleRecordInUse(RecordInUseException ex) {
		return conflict(ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		if (hasConstraint(ex, ACTIVE_PERIOD_OVERLAP_CONSTRAINT, "23P01")) {
			return badRequest("Pricing plan active period overlaps an existing pricing plan");
		}

		if (hasConstraint(ex, ACTIVE_PERIOD_ORDER_CONSTRAINT, CHECK_VIOLATION, H2_CHECK_VIOLATION)) {
			return badRequest("Active Through must be on or after Active From");
		}

		if (hasConstraint(ex, PRICING_PLAN_FEE_PRIMARY_KEY, UNIQUE_VIOLATION)) {
			return badRequest("A pricing plan cannot contain the same fee twice");
		}

		if (hasConstraint(ex, PRICING_PLAN_FEE_REASON_PRIMARY_KEY, UNIQUE_VIOLATION)) {
			return badRequest("A fee cannot contain the same eligibility reason twice");
		}

		if (hasConstraint(ex, FEE_PRODUCT_TYPE_UNIQUE_CONSTRAINT, UNIQUE_VIOLATION)) {
			return badRequest("A fee cannot contain the same product type twice");
		}

		if (hasConstraint(ex, ACCOUNT_ATTRIBUTE_PRODUCT_TYPE_UNIQUE_CONSTRAINT, UNIQUE_VIOLATION)) {
			return badRequest("An account attribute cannot contain the same product type twice");
		}

		if (hasSqlState(ex, UNIQUE_VIOLATION)) {
			return conflict("A record with the same code already exists");
		}

		if (hasSqlState(ex, FOREIGN_KEY_VIOLATION, H2_FOREIGN_KEY_VIOLATION)) {
			return conflict("The request violates a record relationship");
		}

		if (hasSqlState(ex, NOT_NULL_VIOLATION, CHECK_VIOLATION, H2_CHECK_VIOLATION)) {
			return badRequest("The request violates a database constraint");
		}

		if (hasSqlStateClass(ex, "22")) {
			return badRequest("The request data is invalid");
		}

		return conflict("The request violates a database constraint");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("Endpoint was not found"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		return ResponseEntity.internalServerError()
				.body(new ErrorResponse(ex.getMessage() == null ? "An unexpected error occurred" : ex.getMessage()));
	}

	private ResponseEntity<ErrorResponse> badRequest(String message) {
		return ResponseEntity.badRequest().body(new ErrorResponse(message));
	}

	private ResponseEntity<ErrorResponse> conflict(String message) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(message));
	}

	private boolean hasConstraint(Throwable exception, String constraint, String... states) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof SQLException sqlException && sqlException.getMessage() != null
					&& sqlException.getMessage().toLowerCase(Locale.ROOT).contains(constraint)) {
				for (String state : states) {
					if (state.equals(sqlException.getSQLState())) {
						return true;
					}
				}
			}
			cause = cause.getCause();
		}

		return false;
	}

	private boolean hasSqlState(Throwable exception, String... states) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof SQLException sqlException) {
				for (String state : states) {
					if (state.equals(sqlException.getSQLState())) {
						return true;
					}
				}
			}
			cause = cause.getCause();
		}

		return false;
	}

	private boolean hasSqlStateClass(Throwable exception, String sqlStateClass) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof SQLException sqlException && sqlException.getSQLState() != null
					&& sqlException.getSQLState().startsWith(sqlStateClass)) {
				return true;
			}
			cause = cause.getCause();
		}

		return false;
	}
}
