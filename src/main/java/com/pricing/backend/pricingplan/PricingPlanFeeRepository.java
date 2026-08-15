package com.pricing.backend.pricingplan;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingPlanFeeRepository extends JpaRepository<PricingPlanFeeEntity, PricingPlanFeeId> {

	Optional<PricingPlanFeeEntity> findFirstByFee_IdOrderByPricingPlan_PlanCodeAsc(Long feeId);

	Optional<PricingPlanFeeEntity> findFirstByReasons_IdOrderByPricingPlan_PlanCodeAsc(Long reasonId);

	Optional<PricingPlanFeeEntity>
			findFirstByReasons_IdAndPricingPlan_ActiveFromLessThanEqualAndPricingPlan_ActiveThroughGreaterThanEqualOrderByPricingPlan_PlanCodeAsc(
					Long reasonId, LocalDate activeFrom, LocalDate activeThrough);
}
