package com.pricing.backend.pricingplan;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.PricingPlansApi;
import com.pricing.backend.generated.model.PricingPlan;
import com.pricing.backend.generated.model.PricingPlanRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PricingPlanController implements PricingPlansApi {

	private final PricingPlanService pricingPlanService;

	public PricingPlanController(PricingPlanService pricingPlanService) {
		this.pricingPlanService = pricingPlanService;
	}

	@Override
	public ResponseEntity<List<PricingPlan>> listPricingPlans() {
		return ResponseEntity.ok(pricingPlanService.list());
	}

	@Override
	public ResponseEntity<PricingPlan> getPricingPlan(Long id) {
		PricingPlan pricingPlan = pricingPlanService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Pricing plan", id));

		return ResponseEntity.ok(pricingPlan);
	}

	@Override
	public ResponseEntity<PricingPlan> createPricingPlan(PricingPlanRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(pricingPlanService.create(request));
	}

	@Override
	public ResponseEntity<PricingPlan> updatePricingPlan(Long id, PricingPlanRequest request) {
		PricingPlan pricingPlan = pricingPlanService.update(id, request)
				.orElseThrow(() -> new RecordNotFoundException("Pricing plan", id));

		return ResponseEntity.ok(pricingPlan);
	}

	@Override
	public ResponseEntity<Void> deletePricingPlan(Long id) {
		if (pricingPlanService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Pricing plan", id);
	}
}
