package com.pricing.backend.fee;

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

	void validateFeeTypeCanChange(FeeEntity entity, FeeRequest request) {
		if (entity.getFeeType() == request.getFeeType()) {
			return;
		}

		pricingPlanFeeRepository.findFirstByFee_IdOrderByPricingPlan_PlanCodeAsc(entity.getId())
				.ifPresent(pricingPlanFee -> {
					throw new RecordInUseException(
							"fee", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
				});
	}
}
