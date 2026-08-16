package com.pricing.backend.fee;

import java.util.HashSet;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.FeeRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import org.springframework.stereotype.Component;

@Component
class FeeValidator {

	private final PricingPlanFeeRepository pricingPlanFeeRepository;

	FeeValidator(PricingPlanFeeRepository pricingPlanFeeRepository) {
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
	}

	void validateCanUpdate(FeeEntity entity, FeeRequest request) {
		if (hasSameDefinition(entity, request)) {
			return;
		}

		pricingPlanFeeRepository.findFirstByFee_IdOrderByPricingPlan_PlanCodeAsc(entity.getId())
				.ifPresent(pricingPlanFee -> {
					throw new RecordInUseException(
							"fee", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
				});
	}

	private boolean hasSameDefinition(FeeEntity entity, FeeRequest request) {
		return entity.getFeeCode().equals(request.getFeeCode())
				&& entity.getFeeType() == request.getFeeType()
				&& new HashSet<>(entity.getProductTypes()).equals(new HashSet<>(request.getProductTypes()));
	}
}
