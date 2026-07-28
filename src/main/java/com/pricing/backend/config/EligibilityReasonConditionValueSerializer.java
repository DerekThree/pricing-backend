package com.pricing.backend.config;

import java.math.BigDecimal;

import com.pricing.backend.eligibilityreason.EligibilityReasonConditionScalarValue;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class EligibilityReasonConditionValueSerializer extends ValueSerializer<EligibilityReasonConditionScalarValue> {

	@Override
	public void serialize(EligibilityReasonConditionScalarValue value, JsonGenerator generator,
			SerializationContext context) {
		Object scalar = value.getValue();
		if (scalar instanceof String stringValue) {
			generator.writeString(stringValue);
			return;
		}

		if (scalar instanceof BigDecimal decimalValue) {
			generator.writeNumber(decimalValue);
			return;
		}

		if (scalar instanceof Boolean booleanValue) {
			generator.writeBoolean(booleanValue);
			return;
		}

		throw new IllegalArgumentException("Condition value must be a string, number, or boolean");
	}
}
