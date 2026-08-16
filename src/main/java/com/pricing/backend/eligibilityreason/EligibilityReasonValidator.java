package com.pricing.backend.eligibilityreason;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.ReasonRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import com.pricing.backend.simulator.SimulatorDateService;
import org.springframework.stereotype.Component;

@Component
class EligibilityReasonValidator {

	private final PricingPlanFeeRepository pricingPlanFeeRepository;
	private final SimulatorDateService simulatorDateService;
	private final EligibilityReasonNormalizer eligibilityReasonNormalizer;

	EligibilityReasonValidator(PricingPlanFeeRepository pricingPlanFeeRepository,
			SimulatorDateService simulatorDateService,
			EligibilityReasonNormalizer eligibilityReasonNormalizer) {
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.simulatorDateService = simulatorDateService;
		this.eligibilityReasonNormalizer = eligibilityReasonNormalizer;
	}

	void validateCanUpdate(EligibilityReasonEntity entity, ReasonRequest request) {
		LocalDate currentDate = simulatorDateService.getCurrentDate();
		pricingPlanFeeRepository
				.findFirstByReasons_IdAndPricingPlan_ActiveFromLessThanEqualOrderByPricingPlan_PlanCodeAsc(
						entity.getId(), currentDate)
				.ifPresent(pricingPlanFee -> {
					if (!hasSameDefinition(entity, request)) {
						throw new RecordInUseException(
								"eligibility reason", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
					}
				});
	}

	private boolean hasSameDefinition(EligibilityReasonEntity entity, ReasonRequest request) {
		if (!entity.getReasonCode().equals(request.getReasonCode())
				|| entity.getConditions().size() != request.getConditions().size()) {
			return false;
		}

		Set<String> requestConditionKeys = new HashSet<>();
		return request.getConditions().stream().allMatch(requestCondition -> {
			String conditionKey = requestCondition.getAttributeId() + "|"
					+ requestCondition.getOperator().getValue();
			return requestConditionKeys.add(conditionKey) && entity.getConditions().stream()
					.filter(entityCondition -> entityCondition.getId().getAttributeId()
							.equals(requestCondition.getAttributeId())
							&& entityCondition.getId().getOperator().equals(requestCondition.getOperator().getValue()))
					.findFirst()
					.map(entityCondition -> entityCondition.getAttributeValue().equals(
							eligibilityReasonNormalizer.normalizeAttributeValue(
									entityCondition.getAttribute(), requestCondition.getValue())))
					.orElse(false);
		});
	}
}
