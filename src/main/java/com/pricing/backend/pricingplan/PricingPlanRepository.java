package com.pricing.backend.pricingplan;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingPlanRepository extends JpaRepository<PricingPlanEntity, Long> {

	@EntityGraph(attributePaths = {"product", "region"})
	List<PricingPlanEntity> findAllByOrderByPlanCodeAsc();

	@Override
	@EntityGraph(attributePaths = {"product", "region"})
	Optional<PricingPlanEntity> findById(Long id);
}
