package com.pricing.backend.pricingplan;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.pricing.backend.generated.model.PricingPlan;
import com.pricing.backend.generated.model.PricingPlanRequest;
import org.springframework.stereotype.Service;

@Service
public class PricingPlanService {

	private final Map<Long, PricingPlan> pricingPlans = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(3);

	public PricingPlanService() {
		pricingPlans.put(1L, new PricingPlan(
				1L,
				"DC11R112",
				"Free Checking Midwest",
				"1FC00012",
				"Free Checking",
				"MIDWEST1",
				"Midwest",
				OffsetDateTime.parse("2026-06-01T00:00:00+08:00"),
				OffsetDateTime.parse("2026-12-31T23:59:59+08:00"),
				OffsetDateTime.parse("2026-06-06T10:30:00+08:00"),
				"Derek Ochal"
		));
		pricingPlans.put(2L, new PricingPlan(
				2L,
				"PL11R113",
				"Personal Loan South",
				"1PL00056",
				"Personal Loan",
				"SOUTH001",
				"South",
				OffsetDateTime.parse("2026-06-01T00:00:00+08:00"),
				OffsetDateTime.parse("2026-12-31T23:59:59+08:00"),
				OffsetDateTime.parse("2026-06-06T10:45:00+08:00"),
				"John Smith"
		));
	}

	public List<PricingPlan> list() {
		return pricingPlans.values().stream()
				.sorted(Comparator.comparing(PricingPlan::getPlanCode))
				.toList();
	}

	public Optional<PricingPlan> get(Long id) {
		return Optional.ofNullable(pricingPlans.get(id));
	}

	public PricingPlan create(PricingPlanRequest request) {
		PricingPlan pricingPlan = new PricingPlan(
				nextId.getAndIncrement(),
				request.getPlanCode(),
				request.getPlanName(),
				request.getProductCode(),
				request.getProductName(),
				request.getRegionCode(),
				request.getRegionName(),
				request.getActiveFrom(),
				request.getActiveTo(),
				now(),
				request.getUpdatedBy()
		);
		pricingPlans.put(pricingPlan.getId(), pricingPlan);
		return pricingPlan;
	}

	public Optional<PricingPlan> update(Long id, PricingPlanRequest request) {
		Optional<PricingPlan> existingPricingPlan = get(id);
		if (existingPricingPlan.isEmpty()) {
			return Optional.empty();
		}

		PricingPlan pricingPlan = new PricingPlan(
				id,
				request.getPlanCode(),
				request.getPlanName(),
				request.getProductCode(),
				request.getProductName(),
				request.getRegionCode(),
				request.getRegionName(),
				request.getActiveFrom(),
				request.getActiveTo(),
				now(),
				request.getUpdatedBy()
		);
		pricingPlans.put(id, pricingPlan);
		return Optional.of(pricingPlan);
	}

	public boolean delete(Long id) {
		return pricingPlans.remove(id) != null;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now();
	}
}
