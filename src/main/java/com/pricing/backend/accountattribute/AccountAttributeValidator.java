package com.pricing.backend.accountattribute;

import java.time.LocalDate;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.generated.model.AttributeRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import com.pricing.backend.simulator.SimulatorDateService;
import org.springframework.stereotype.Component;

@Component
class AccountAttributeValidator {

	private final PricingPlanFeeRepository pricingPlanFeeRepository;
	private final SimulatorDateService simulatorDateService;

	AccountAttributeValidator(PricingPlanFeeRepository pricingPlanFeeRepository,
			SimulatorDateService simulatorDateService) {
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.simulatorDateService = simulatorDateService;
	}

	void validateCanUpdate(AccountAttributeEntity entity, AttributeRequest request) {
		if (hasSameDefinition(entity, request)) {
			return;
		}

		LocalDate currentDate = simulatorDateService.getCurrentDate();
		pricingPlanFeeRepository
				.findFirstByReasons_Conditions_Attribute_IdAndPricingPlan_ActiveFromLessThanEqualOrderByPricingPlan_PlanCodeAsc(
						entity.getId(), currentDate)
				.ifPresent(pricingPlanFee -> {
					EligibilityReasonEntity reason = pricingPlanFee.getReasons().stream()
							.filter(candidate -> candidate.getConditions().stream()
									.anyMatch(condition -> condition.getId().getAttributeId().equals(entity.getId())))
							.findFirst()
							.orElseThrow();
					throw new RecordInUseException(
							"account attribute", "eligibility reason", reason.getReasonCode());
				});
	}

	private boolean hasSameDefinition(AccountAttributeEntity entity, AttributeRequest request) {
		return entity.getAttributeCode().equals(request.getAttributeCode())
				&& entity.getAttributeType() == request.getAttributeType();
	}
}
