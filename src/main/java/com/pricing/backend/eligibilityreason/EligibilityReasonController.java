package com.pricing.backend.eligibilityreason;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.EligibilityReasonsApi;
import com.pricing.backend.generated.model.ReasonDetail;
import com.pricing.backend.generated.model.ReasonListItem;
import com.pricing.backend.generated.model.ReasonOptions;
import com.pricing.backend.generated.model.ReasonRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EligibilityReasonController implements EligibilityReasonsApi {

	private final EligibilityReasonService eligibilityReasonService;

	public EligibilityReasonController(EligibilityReasonService eligibilityReasonService) {
		this.eligibilityReasonService = eligibilityReasonService;
	}

	@Override
	public ResponseEntity<List<ReasonListItem>> listReasons() {
		return ResponseEntity.ok(eligibilityReasonService.list());
	}

	@Override
	public ResponseEntity<ReasonDetail> getReason(Long id) {
		ReasonDetail eligibilityReason = eligibilityReasonService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Eligibility reason", id));

		return ResponseEntity.ok(eligibilityReason);
	}

	@Override
	public ResponseEntity<ReasonOptions> getReasonOptions() {
		return ResponseEntity.ok(eligibilityReasonService.getOptions());
	}

	@Override
	public ResponseEntity<ReasonDetail> createReason(ReasonRequest eligibilityReasonRequest) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(eligibilityReasonService.create(eligibilityReasonRequest));
	}

	@Override
	public ResponseEntity<ReasonDetail> updateReason(Long id, ReasonRequest eligibilityReasonRequest) {
		ReasonDetail eligibilityReason = eligibilityReasonService
				.update(id, eligibilityReasonRequest)
				.orElseThrow(() -> new RecordNotFoundException("Eligibility reason", id));

		return ResponseEntity.ok(eligibilityReason);
	}

	@Override
	public ResponseEntity<Void> deleteReason(Long id) {
		if (eligibilityReasonService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Eligibility reason", id);
	}
}
