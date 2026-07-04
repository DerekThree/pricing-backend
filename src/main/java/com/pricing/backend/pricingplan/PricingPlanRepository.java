package com.pricing.backend.pricingplan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingPlanRepository extends JpaRepository<PricingPlanEntity, Long> {

	List<PricingPlanEntity> findAllByOrderByPlanCodeAsc();
}
