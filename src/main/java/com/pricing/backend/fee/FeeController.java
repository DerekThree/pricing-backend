package com.pricing.backend.fee;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.FeesApi;
import com.pricing.backend.generated.model.FeeDetail;
import com.pricing.backend.generated.model.FeeListItem;
import com.pricing.backend.generated.model.FeeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeeController implements FeesApi {

	private final FeeService feeService;

	public FeeController(FeeService feeService) {
		this.feeService = feeService;
	}

	@Override
	public ResponseEntity<List<FeeListItem>> listFees() {
		return ResponseEntity.ok(feeService.list());
	}

	@Override
	public ResponseEntity<FeeDetail> getFee(Long id) {
		FeeDetail fee = feeService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Fee", id));

		return ResponseEntity.ok(fee);
	}

	@Override
	public ResponseEntity<FeeDetail> createFee(FeeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(feeService.create(request));
	}

	@Override
	public ResponseEntity<FeeDetail> updateFee(Long id, FeeRequest request) {
		FeeDetail fee = feeService.update(id, request)
				.orElseThrow(() -> new RecordNotFoundException("Fee", id));

		return ResponseEntity.ok(fee);
	}

	@Override
	public ResponseEntity<Void> deleteFee(Long id) {
		if (feeService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Fee", id);
	}
}
