package com.pricing.backend.accountattribute;

import java.util.HashSet;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.generated.model.AttributeRequest;
import org.springframework.stereotype.Component;

@Component
class AccountAttributeValidator {

	private final EligibilityReasonRepository eligibilityReasonRepository;

	AccountAttributeValidator(EligibilityReasonRepository eligibilityReasonRepository) {
		this.eligibilityReasonRepository = eligibilityReasonRepository;
	}

	void validateCanUpdate(AccountAttributeEntity entity, AttributeRequest request) {
		if (hasSameDefinition(entity, request)) {
			return;
		}

		eligibilityReasonRepository.findFirstByConditions_Attribute_IdOrderByReasonCodeAsc(entity.getId())
				.ifPresent(reason -> {
					throw new RecordInUseException(
							"account attribute", "eligibility reason", reason.getReasonCode());
				});
	}

	private boolean hasSameDefinition(AccountAttributeEntity entity, AttributeRequest request) {
		return entity.getAttributeCode().equals(request.getAttributeCode())
				&& entity.getAttributeType() == request.getAttributeType()
				&& new HashSet<>(entity.getProductTypes()).equals(new HashSet<>(request.getProductTypes()));
	}
}
