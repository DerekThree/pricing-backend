package com.pricing.backend.config;

import com.pricing.backend.batch.AccountAttributeValueDeserializer;
import com.pricing.backend.eligibilityreason.EligibilityReasonConditionScalarValue;
import com.pricing.backend.generated.model.AccountAttributeValue;
import com.pricing.backend.generated.model.ReasonConditionValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

	@Bean
	public SimpleModule scalarValueModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ReasonConditionValue.class, new EligibilityReasonConditionValueDeserializer());
		module.addSerializer(EligibilityReasonConditionScalarValue.class,
				new EligibilityReasonConditionValueSerializer());
		module.addDeserializer(AccountAttributeValue.class, new AccountAttributeValueDeserializer());
		return module;
	}
}
