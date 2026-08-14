package com.pricing.backend.branch;

import com.pricing.backend.generated.model.BranchDetail;
import com.pricing.backend.generated.model.BranchListItem;
import org.springframework.stereotype.Component;

@Component
class BranchMapper {

	BranchListItem toBranchListItem(BranchEntity entity) {
		return new BranchListItem(
				entity.getId(),
				formatCodeAndName(entity.getBranchCode(), entity.getBranchName()),
				entity.getState(),
				entity.getZipCode(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	BranchDetail toBranchDetail(BranchEntity entity) {
		return new BranchDetail(
				entity.getBranchCode(),
				entity.getBranchName(),
				entity.getState(),
				entity.getZipCode(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
