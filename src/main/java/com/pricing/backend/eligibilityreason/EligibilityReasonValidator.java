package com.pricing.backend.eligibilityreason;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.generated.model.ReasonRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import org.springframework.stereotype.Component;

@Component
class EligibilityReasonValidator {

	private final PricingPlanFeeRepository pricingPlanFeeRepository;
	private final EligibilityReasonNormalizer eligibilityReasonNormalizer;

	EligibilityReasonValidator(PricingPlanFeeRepository pricingPlanFeeRepository,
			EligibilityReasonNormalizer eligibilityReasonNormalizer) {
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.eligibilityReasonNormalizer = eligibilityReasonNormalizer;
	}

	void validateCanUpdate(EligibilityReasonEntity entity, ReasonRequest request) {
		pricingPlanFeeRepository.findFirstByReasons_IdOrderByPricingPlan_PlanCodeAsc(entity.getId())
				.ifPresent(pricingPlanFee -> {
					if (!hasSameDefinition(entity, request)) {
						throw new RecordInUseException(
								"eligibility reason", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
					}
				});
	}

	void validateProductTypes(Collection<AccountAttributeEntity> attributes) {
		Set<ProductType> productTypes = EnumSet.allOf(ProductType.class);
		for (AccountAttributeEntity attribute : attributes) {
			productTypes.retainAll(attribute.getProductTypes());
		}

		if (productTypes.isEmpty()) {
			throw new IllegalArgumentException("Applicable Product Types must contain at least one item");
		}
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
					.filter(entityCondition -> entityCondition.getAttribute().getId()
							.equals(requestCondition.getAttributeId())
							&& entityCondition.getOperator().equals(requestCondition.getOperator().getValue()))
					.findFirst()
					.map(entityCondition -> entityCondition.getAttributeValue().equals(
							eligibilityReasonNormalizer.normalizeAttributeValue(
									entityCondition.getAttribute(), requestCondition.getValue())))
					.orElse(false);
		});
	}
}
