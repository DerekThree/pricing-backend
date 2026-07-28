package com.pricing.backend.eligibilityreason;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.EligibilityReasonsApi;
import com.pricing.backend.generated.model.EligibilityReasonDetail;
import com.pricing.backend.generated.model.EligibilityReasonListItem;
import com.pricing.backend.generated.model.EligibilityReasonOptions;
import com.pricing.backend.generated.model.EligibilityReasonRequest;
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
	public ResponseEntity<List<EligibilityReasonListItem>> listEligibilityReasons() {
		return ResponseEntity.ok(eligibilityReasonService.list());
	}

	@Override
	public ResponseEntity<EligibilityReasonDetail> getEligibilityReason(Long id) {
		EligibilityReasonDetail eligibilityReason = eligibilityReasonService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Eligibility reason", id));

		return ResponseEntity.ok(eligibilityReason);
	}

	@Override
	public ResponseEntity<EligibilityReasonOptions> getEligibilityReasonOptions() {
		return ResponseEntity.ok(eligibilityReasonService.getOptions());
	}

	@Override
	public ResponseEntity<EligibilityReasonDetail> createEligibilityReason(
			EligibilityReasonRequest eligibilityReasonRequest) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(eligibilityReasonService.create(eligibilityReasonRequest));
	}

	@Override
	public ResponseEntity<EligibilityReasonDetail> updateEligibilityReason(Long id,
			EligibilityReasonRequest eligibilityReasonRequest) {
		EligibilityReasonDetail eligibilityReason = eligibilityReasonService
				.update(id, eligibilityReasonRequest)
				.orElseThrow(() -> new RecordNotFoundException("Eligibility reason", id));

		return ResponseEntity.ok(eligibilityReason);
	}

	@Override
	public ResponseEntity<Void> deleteEligibilityReason(Long id) {
		if (eligibilityReasonService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Eligibility reason", id);
	}
}
