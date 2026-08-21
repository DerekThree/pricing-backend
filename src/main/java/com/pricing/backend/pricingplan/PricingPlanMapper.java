package com.pricing.backend.pricingplan;

import java.util.Comparator;
import java.util.List;

import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.generated.model.FeeOption;
import com.pricing.backend.generated.model.PricingPlanDetail;
import com.pricing.backend.generated.model.PricingPlanFee;
import com.pricing.backend.generated.model.PricingPlanListItem;
import com.pricing.backend.generated.model.PricingPlanOptions;
import com.pricing.backend.generated.model.ProductOption;
import com.pricing.backend.generated.model.ReasonOption;
import com.pricing.backend.generated.model.RegionOption;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.region.RegionEntity;
import org.springframework.stereotype.Component;

@Component
class PricingPlanMapper {

	PricingPlanListItem toPricingPlanListItem(PricingPlanEntity entity) {
		return new PricingPlanListItem(
				entity.getId(),
				formatCodeAndName(entity.getPlanCode(), entity.getPlanName()),
				formatCodeAndName(entity.getProduct().getProductCode(), entity.getProduct().getProductName()),
				formatCodeAndName(entity.getRegion().getRegionCode(), entity.getRegion().getRegionName()),
				entity.getActiveFrom(),
				entity.getActiveThrough(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	PricingPlanDetail toPricingPlanDetail(PricingPlanEntity entity) {
		return new PricingPlanDetail(
				entity.getPlanCode(),
				entity.getPlanName(),
				entity.getProduct().getId(),
				entity.getRegion().getId(),
				entity.getActiveFrom(),
				entity.getActiveThrough(),
				entity.getFees().stream()
						.sorted(pricingPlanFeeComparator())
						.map(this::toPricingPlanFee)
						.toList(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn(),
				new PricingPlanOptions(List.of(toProductOption(entity.getProduct())),
						List.of(toRegionOption(entity.getRegion())))
						.fees(entity.getFees().stream()
								.sorted(pricingPlanFeeComparator())
								.map(PricingPlanFeeEntity::getFee)
								.map(this::toFeeOption)
								.toList())
						.reasons(entity.getFees().stream()
								.flatMap(fee -> fee.getReasons().stream())
								.distinct()
								.sorted(reasonComparator())
								.map(this::toReasonOption)
								.toList())
						.intervals(null)
		);
	}

	PricingPlanFee toPricingPlanFee(PricingPlanFeeEntity entity) {
		return new PricingPlanFee(
				entity.getFee().getId(), entity.getAmount(), entity.getReasons().stream()
						.sorted(reasonComparator())
						.map(reason -> reason.getId())
						.toList());
	}

	ProductOption toProductOption(ProductEntity product) {
		return new ProductOption(
				product.getId(),
				product.getProductCode(),
				product.getProductName(),
				product.getProductType()
		);
	}

	RegionOption toRegionOption(RegionEntity region) {
		return new RegionOption(
				region.getId(),
				region.getRegionCode(),
				region.getRegionName()
		);
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

	ReasonOption toReasonOption(EligibilityReasonEntity reason) {
		return new ReasonOption(
				reason.getId(),
				reason.getReasonCode(),
				reason.getReasonName(),
				reason.getProductTypes()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}

	private Comparator<PricingPlanFeeEntity> pricingPlanFeeComparator() {
		return Comparator.comparing((PricingPlanFeeEntity fee) -> fee.getFee().getFeeCode());
	}

	private Comparator<EligibilityReasonEntity> reasonComparator() {
		return Comparator.comparing(EligibilityReasonEntity::getReasonCode);
	}
}
