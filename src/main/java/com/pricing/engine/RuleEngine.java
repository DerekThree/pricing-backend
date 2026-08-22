package com.pricing.engine;

public interface RuleEngine {

	AccountBatchResult price(AccountBatch batch);
}
