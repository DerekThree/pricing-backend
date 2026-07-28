package com.pricing.backend.config;

import com.pricing.backend.eligibilityreason.EligibilityReasonConditionScalarValue;
import com.pricing.backend.generated.model.EligibilityReasonConditionValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

	@Bean
	public SimpleModule eligibilityReasonConditionValueModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(EligibilityReasonConditionValue.class,
				new EligibilityReasonConditionValueDeserializer());
		module.addSerializer(EligibilityReasonConditionScalarValue.class,
				new EligibilityReasonConditionValueSerializer());
		return module;
	}
}
