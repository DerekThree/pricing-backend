package com.pricing.backend.eligibilityreason;

import java.time.LocalDate;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import com.pricing.backend.simulator.SimulatorDateService;
import org.springframework.stereotype.Component;

@Component
class EligibilityReasonValidator {

	private final PricingPlanFeeRepository pricingPlanFeeRepository;
	private final SimulatorDateService simulatorDateService;

	EligibilityReasonValidator(PricingPlanFeeRepository pricingPlanFeeRepository,
			SimulatorDateService simulatorDateService) {
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.simulatorDateService = simulatorDateService;
	}

	void validateCanUpdate(EligibilityReasonEntity entity) {
		LocalDate currentDate = simulatorDateService.getCurrentDate();
		pricingPlanFeeRepository
				.findFirstByReasons_IdAndPricingPlan_ActiveFromLessThanEqualAndPricingPlan_ActiveThroughGreaterThanEqualOrderByPricingPlan_PlanCodeAsc(
						entity.getId(), currentDate, currentDate)
				.ifPresent(pricingPlanFee -> {
					throw new RecordInUseException(
							"eligibility reason", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
				});
	}
}
