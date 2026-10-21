package com.pricing.backend.batch;

import com.pricing.backend.generated.api.BatchApi;
import com.pricing.backend.generated.model.BatchRequest;
import com.pricing.backend.generated.model.BatchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchController implements BatchApi {

	private final BatchService batchService;

	public BatchController(BatchService batchService) {
		this.batchService = batchService;
	}

	@Override
	public ResponseEntity<BatchResult> postBatch(BatchRequest request) {
		return ResponseEntity.ok(batchService.price(request));
	}
}
