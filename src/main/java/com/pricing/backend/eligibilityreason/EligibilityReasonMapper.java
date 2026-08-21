package com.pricing.backend.eligibilityreason;

import java.math.BigDecimal;
import java.util.Comparator;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.generated.model.AttributeOption;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.ReasonCondition;
import com.pricing.backend.generated.model.ReasonConditionValue;
import com.pricing.backend.generated.model.ReasonDetail;
import com.pricing.backend.generated.model.ReasonListItem;
import com.pricing.backend.generated.model.ReasonOperator;
import com.pricing.backend.generated.model.ReasonOptions;
import org.springframework.stereotype.Component;

@Component
class EligibilityReasonMapper {

	ReasonListItem toReasonListItem(EligibilityReasonEntity entity) {
		return new ReasonListItem(
				entity.getId(),
				formatCodeAndName(entity.getReasonCode(), entity.getReasonName()),
				entity.getConditions().stream()
						.sorted(conditionComparator())
						.map(this::toConditionSummary)
						.toList(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	ReasonDetail toReasonDetail(EligibilityReasonEntity entity) {
		return new ReasonDetail(
				entity.getReasonCode(),
				entity.getReasonName(),
				entity.getConditions().stream()
						.sorted(conditionComparator())
						.map(this::toReasonCondition)
						.toList(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn(),
				new ReasonOptions(
						entity.getConditions().stream()
								.map(EligibilityReasonConditionEntity::getAttribute)
								.distinct()
								.sorted(Comparator.comparing(AccountAttributeEntity::getAttributeCode))
								.map(this::toAttributeOption)
								.toList()
				)
		);
	}

	ReasonCondition toReasonCondition(EligibilityReasonConditionEntity entity) {
		return new ReasonCondition(
				entity.getAttribute().getId(),
				ReasonOperator.fromValue(entity.getOperator()),
				toReasonConditionValue(entity.getAttribute().getAttributeType(), entity.getAttributeValue())
		);
	}

	AttributeOption toAttributeOption(AccountAttributeEntity attribute) {
		return new AttributeOption(
				attribute.getId(),
				attribute.getAttributeCode(),
				attribute.getAttributeName(),
				attribute.getAttributeType(),
				attribute.getProductTypes().stream().toList()
		);
	}

	private ReasonConditionValue toReasonConditionValue(AttributeType type, String value) {
		return switch (type) {
			case BOOLEAN -> new EligibilityReasonConditionScalarValue(Boolean.valueOf(value));
			case DATE, TEXT -> new EligibilityReasonConditionScalarValue(value);
			case DECIMAL, INTEGER -> new EligibilityReasonConditionScalarValue(new BigDecimal(value));
		};
	}

	private Comparator<EligibilityReasonConditionEntity> conditionComparator() {
		return Comparator.comparing(
				(EligibilityReasonConditionEntity condition) -> condition.getAttribute().getAttributeCode())
				.thenComparing(EligibilityReasonConditionEntity::getOperator);
	}

	private String toConditionSummary(EligibilityReasonConditionEntity entity) {
		return entity.getAttribute().getAttributeCode() + " " + entity.getOperator() + " "
				+ entity.getAttributeValue();
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
