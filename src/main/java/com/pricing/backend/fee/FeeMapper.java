package com.pricing.backend.fee;

import com.pricing.backend.generated.model.FeeDetail;
import com.pricing.backend.generated.model.FeeListItem;
import org.springframework.stereotype.Component;

@Component
class FeeMapper {

	FeeListItem toFeeListItem(FeeEntity entity) {
		return new FeeListItem(
				entity.getId(),
				formatCodeAndName(entity.getFeeCode(), entity.getFeeName()),
				entity.getFeeType(),
				entity.getProductTypes(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	FeeDetail toFeeDetail(FeeEntity entity) {
		return new FeeDetail(
				entity.getFeeCode(),
				entity.getFeeName(),
				entity.getFeeType(),
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
