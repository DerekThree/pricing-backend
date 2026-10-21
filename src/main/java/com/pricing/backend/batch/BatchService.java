package com.pricing.backend.batch;

import com.pricing.backend.generated.model.BatchRequest;
import com.pricing.backend.generated.model.BatchResult;
import com.pricing.engine.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BatchService {

	private static final Logger log = LoggerFactory.getLogger(BatchService.class);

	private final RuleEngine ruleEngine;
	private final BatchMapper mapper;
	private final BatchValidator validator;

	public BatchService(RuleEngine ruleEngine, BatchMapper mapper, BatchValidator validator) {
		this.ruleEngine = ruleEngine;
		this.mapper = mapper;
		this.validator = validator;
	}

	public BatchResult price(BatchRequest request) {
		log.info("Pricing Batch received: {}", request.getBatchId());
		validator.validate(request);
		BatchResult result = mapper.toResponse(ruleEngine.price(mapper.toRuleEngineBatch(request)));
		log.info("Pricing Batch evaluation completed: {}", request.getBatchId());
		return result;
	}
}
