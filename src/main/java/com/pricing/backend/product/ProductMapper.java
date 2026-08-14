package com.pricing.backend.product;

import com.pricing.backend.generated.model.ProductDetail;
import com.pricing.backend.generated.model.ProductListItem;
import org.springframework.stereotype.Component;

@Component
class ProductMapper {

	ProductListItem toProductListItem(ProductEntity entity) {
		return new ProductListItem(
				entity.getId(),
				formatCodeAndName(entity.getProductCode(), entity.getProductName()),
				entity.getProductType(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	ProductDetail toProductDetail(ProductEntity entity) {
		return new ProductDetail(
				entity.getProductCode(),
				entity.getProductName(),
				entity.getProductType(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
