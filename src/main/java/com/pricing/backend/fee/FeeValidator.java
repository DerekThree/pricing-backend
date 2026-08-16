package com.pricing.backend.fee;

import java.time.LocalDate;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.FeeRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import com.pricing.backend.simulator.SimulatorDateService;
import org.springframework.stereotype.Component;

@Component
class FeeValidator {

	private final PricingPlanFeeRepository pricingPlanFeeRepository;
	private final SimulatorDateService simulatorDateService;

	FeeValidator(PricingPlanFeeRepository pricingPlanFeeRepository, SimulatorDateService simulatorDateService) {
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.simulatorDateService = simulatorDateService;
	}

	void validateCanUpdate(FeeEntity entity, FeeRequest request) {
		if (hasSameDefinition(entity, request)) {
			return;
		}

		LocalDate currentDate = simulatorDateService.getCurrentDate();
		pricingPlanFeeRepository
				.findFirstByFee_IdAndPricingPlan_ActiveFromLessThanEqualOrderByPricingPlan_PlanCodeAsc(
						entity.getId(), currentDate)
				.ifPresent(pricingPlanFee -> {
					throw new RecordInUseException(
							"fee", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
				});
	}

	private boolean hasSameDefinition(FeeEntity entity, FeeRequest request) {
		return entity.getFeeCode().equals(request.getFeeCode())
				&& entity.getFeeType() == request.getFeeType()
				&& entity.getProductTypes().equals(request.getProductTypes());
	}
}
