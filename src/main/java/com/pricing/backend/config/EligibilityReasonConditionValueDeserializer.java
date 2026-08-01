package com.pricing.backend.config;

import java.math.BigDecimal;

import com.pricing.backend.eligibilityreason.EligibilityReasonConditionScalarValue;
import com.pricing.backend.generated.model.ReasonConditionValue;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public class EligibilityReasonConditionValueDeserializer extends ValueDeserializer<ReasonConditionValue> {

	@Override
	public ReasonConditionValue deserialize(JsonParser parser, DeserializationContext context) {
		JsonToken token = parser.currentToken();
		if (token == JsonToken.VALUE_STRING) {
			return new EligibilityReasonConditionScalarValue(parser.getText());
		}

		if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
			return new EligibilityReasonConditionScalarValue(parser.getDecimalValue());
		}

		if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
			return new EligibilityReasonConditionScalarValue(parser.getBooleanValue());
		}

		throw InvalidFormatException.from(parser,
				"Condition value must be a string, number, or boolean",
				parser.readValueAsTree(),
				Object.class);
	}
}
