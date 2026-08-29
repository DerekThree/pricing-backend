package com.pricing.backend.simulator;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.generated.model.AttributeOption;
import com.pricing.backend.generated.model.BranchOption;
import com.pricing.backend.generated.model.FeeOption;
import com.pricing.backend.generated.model.ProductOption;
import com.pricing.backend.product.ProductEntity;
import org.springframework.stereotype.Component;

@Component
class SimulatorMapper {

	ProductOption toProductOption(ProductEntity product) {
		return new ProductOption(
				product.getId(),
				product.getProductCode(),
				product.getProductName(),
				product.getProductType()
		);
	}

	BranchOption toBranchOption(BranchEntity branch) {
		return new BranchOption(branch.getId(), branch.getBranchCode(), branch.getBranchName());
	}

	FeeOption toFeeOption(FeeEntity fee) {
		return new FeeOption(
				fee.getId(),
				fee.getFeeCode(),
				fee.getFeeName(),
				fee.getFeeType(),
				fee.getProductTypes()
		);
	}

	AttributeOption toAttributeOption(AccountAttributeEntity attribute) {
		return new AttributeOption(
				attribute.getId(),
				attribute.getAttributeCode(),
				attribute.getAttributeName(),
				attribute.getAttributeType(),
				attribute.getProductTypes()
		);
	}
}
