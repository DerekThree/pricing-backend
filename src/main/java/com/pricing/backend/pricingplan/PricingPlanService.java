package com.pricing.backend.pricingplan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.PricingPlan;
import com.pricing.backend.generated.model.PricingPlanRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingPlanService {

	private final PricingPlanRepository pricingPlanRepository;

	public PricingPlanService(PricingPlanRepository pricingPlanRepository) {
		this.pricingPlanRepository = pricingPlanRepository;
	}

	@Transactional(readOnly = true)
	public List<PricingPlan> list() {
		return pricingPlanRepository.findAllByOrderByPlanCodeAsc().stream()
				.map(this::toModel)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<PricingPlan> get(Long id) {
		return pricingPlanRepository.findById(id).map(this::toModel);
	}

	@Transactional
	public PricingPlan create(PricingPlanRequest request) {
		PricingPlanEntity entity = new PricingPlanEntity();
		apply(entity, request);
		return toModel(pricingPlanRepository.save(entity));
	}

	@Transactional
	public Optional<PricingPlan> update(Long id, PricingPlanRequest request) {
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toModel(pricingPlanRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!pricingPlanRepository.existsById(id)) {
			return false;
		}

		pricingPlanRepository.deleteById(id);
		return true;
	}

	private void apply(PricingPlanEntity entity, PricingPlanRequest request) {
		entity.setPlanCode(request.getPlanCode());
		entity.setPlanName(request.getPlanName());
		entity.setProductCode(request.getProductCode());
		entity.setProductName(request.getProductName());
		entity.setRegionCode(request.getRegionCode());
		entity.setRegionName(request.getRegionName());
		entity.setActiveFrom(request.getActiveFrom());
		entity.setActiveTo(request.getActiveTo());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private PricingPlan toModel(PricingPlanEntity entity) {
		return new PricingPlan(
				entity.getId(),
				entity.getPlanCode(),
				entity.getPlanName(),
				entity.getProductCode(),
				entity.getProductName(),
				entity.getRegionCode(),
				entity.getRegionName(),
				entity.getActiveFrom(),
				entity.getActiveTo(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}
}
