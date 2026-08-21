package com.pricing.backend.accountattribute;

import com.pricing.backend.generated.model.AttributeDetail;
import com.pricing.backend.generated.model.AttributeListItem;
import org.springframework.stereotype.Component;

@Component
class AccountAttributeMapper {

	AttributeListItem toAttributeListItem(AccountAttributeEntity entity) {
		return new AttributeListItem(
				entity.getId(),
				formatCodeAndName(entity.getAttributeCode(), entity.getAttributeName()),
				entity.getAttributeType(),
				entity.getProductTypes(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	AttributeDetail toAttributeDetail(AccountAttributeEntity entity) {
		return new AttributeDetail(
				entity.getAttributeCode(),
				entity.getAttributeName(),
				entity.getAttributeType(),
				entity.getProductTypes(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
