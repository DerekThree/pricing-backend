package com.pricing.backend.eligibilityreason;

import java.math.BigDecimal;
import java.util.Objects;

import com.pricing.backend.generated.model.ReasonConditionValue;

public final class EligibilityReasonConditionScalarValue implements ReasonConditionValue {

	private final Object value;

	public EligibilityReasonConditionScalarValue(Object value) {
		if (!(value instanceof String) && !(value instanceof BigDecimal) && !(value instanceof Boolean)) {
			throw new IllegalArgumentException("Condition value must be a string, number, or boolean");
		}

		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof EligibilityReasonConditionScalarValue other)) {
			return false;
		}

		return Objects.equals(value, other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
