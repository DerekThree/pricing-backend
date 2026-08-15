package com.pricing.backend.eligibilityreason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.generated.model.ReasonConditionValue;
import org.springframework.stereotype.Component;

@Component
class EligibilityReasonNormalizer {

	String normalizeAttributeValue(AccountAttributeEntity attribute, ReasonConditionValue value) {
		Object scalar = extractScalarValue(value);
		return switch (attribute.getAttributeType()) {
			case BOOLEAN -> normalizeBooleanValue(attribute, scalar);
			case DATE -> normalizeDateValue(attribute, scalar);
			case DECIMAL -> normalizeDecimalValue(attribute, scalar);
			case INTEGER -> normalizeIntegerValue(attribute, scalar);
			case TEXT -> normalizeTextValue(attribute, scalar);
		};
	}

	String serializeScalarValue(ReasonConditionValue value) {
		return extractScalarValue(value).toString();
	}

	private String normalizeBooleanValue(AccountAttributeEntity attribute, Object value) {
		if (value instanceof Boolean booleanValue) {
			return booleanValue.toString();
		}

		throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be a boolean");
	}

	private String normalizeDateValue(AccountAttributeEntity attribute, Object value) {
		if (!(value instanceof String stringValue)) {
			throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be a date string");
		}

		try {
			return LocalDate.parse(stringValue).toString();
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("Condition value for account attribute "
					+ attribute.getAttributeCode() + " must be a valid ISO date");
		}
	}

	private String normalizeDecimalValue(AccountAttributeEntity attribute, Object value) {
		if (value instanceof BigDecimal decimalValue) {
			return decimalValue.stripTrailingZeros().toPlainString();
		}

		throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be a number");
	}

	private String normalizeIntegerValue(AccountAttributeEntity attribute, Object value) {
		if (!(value instanceof BigDecimal decimalValue)) {
			throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be an integer");
		}

		try {
			return decimalValue.toBigIntegerExact().toString();
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Condition value for account attribute "
					+ attribute.getAttributeCode() + " must be an integer");
		}
	}

	private String normalizeTextValue(AccountAttributeEntity attribute, Object value) {
		if (value instanceof String stringValue) {
			return stringValue;
		}

		throw new IllegalArgumentException("Condition value for account attribute "
				+ attribute.getAttributeCode() + " must be a string");
	}

	private Object extractScalarValue(ReasonConditionValue value) {
		if (value instanceof EligibilityReasonConditionScalarValue scalarValue) {
			return scalarValue.getValue();
		}

		throw new IllegalArgumentException("Condition value format is not supported");
	}
}
