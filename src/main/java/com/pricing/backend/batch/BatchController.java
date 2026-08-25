package com.pricing.backend.batch;

import com.pricing.backend.generated.api.BatchApi;
import com.pricing.backend.generated.model.BatchRequest;
import com.pricing.backend.generated.model.BatchResult;
import com.pricing.engine.RuleEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchController implements BatchApi {

	private final RuleEngine ruleEngine;
	private final BatchMapper mapper;
	private final BatchValidator validator;

	public BatchController(RuleEngine ruleEngine, BatchMapper mapper, BatchValidator validator) {
		this.ruleEngine = ruleEngine;
		this.mapper = mapper;
		this.validator = validator;
	}

	@Override
	public ResponseEntity<BatchResult> batchPost(BatchRequest request) {
		validator.validate(request);
		return ResponseEntity.ok(mapper.toResponse(ruleEngine.price(mapper.toAccountBatch(request))));
	}
}
