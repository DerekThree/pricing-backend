package com.pricing.engine;

public class PricingConfigurationAccessException extends RuntimeException {

	public PricingConfigurationAccessException(Throwable cause) {
		super("Pricing Configuration is unavailable", cause);
	}
}
