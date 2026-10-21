package com.pricing.engine;

public interface RuleEngine {

	RuleEngineBatchResult price(RuleEngineBatch batch);
}
