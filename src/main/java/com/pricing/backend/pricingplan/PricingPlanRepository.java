package com.pricing.backend.pricingplan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingPlanRepository extends JpaRepository<PricingPlanEntity, Long> {

	@EntityGraph(attributePaths = {"product", "region"})
	List<PricingPlanEntity> findAllByOrderByPlanCodeAsc();

	@Override
	@EntityGraph(attributePaths = {"product", "region", "fees", "fees.fee", "fees.fee.productTypes", "fees.reasons"})
	Optional<PricingPlanEntity> findById(Long id);

	Optional<PricingPlanEntity> findFirstByProductIdOrderByPlanCodeAsc(Long productId);

	Optional<PricingPlanEntity> findFirstByRegionIdOrderByPlanCodeAsc(Long regionId);

	List<PricingPlanEntity> findAllByProductIdAndRegionIdAndActiveThroughGreaterThanEqual(
			Long productId, Long regionId, LocalDate activeThrough);

}
