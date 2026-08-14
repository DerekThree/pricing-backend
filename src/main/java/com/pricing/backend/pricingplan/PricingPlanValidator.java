package com.pricing.backend.pricingplan;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.generated.model.PricingPlanRequest;
import com.pricing.backend.simulator.SimulatorDateService;
import org.springframework.stereotype.Component;

@Component
class PricingPlanValidator {

	private final SimulatorDateService simulatorDateService;

	PricingPlanValidator(SimulatorDateService simulatorDateService) {
		this.simulatorDateService = simulatorDateService;
	}

	void validate(PricingPlanEntity entity, PricingPlanRequest request) {
		LocalDate currentDate = simulatorDateService.getCurrentDate();
		validateLifecycle(entity, request, currentDate);
		if (entity.getId() == null && request.getActiveFrom().isBefore(currentDate)) {
			throw new IllegalArgumentException("activeFrom must be on or after the application current date");
		}
	}

	void validateCanDelete(PricingPlanEntity entity) {
		if (isActive(entity, simulatorDateService.getCurrentDate())) {
			throw new IllegalArgumentException("Active pricing plans cannot be deleted");
		}
	}

	private void validateLifecycle(PricingPlanEntity entity, PricingPlanRequest request, LocalDate currentDate) {
		if (entity.getId() == null || currentDate.isBefore(entity.getActiveFrom())) {
			return;
		}

		if (isActive(entity, currentDate)) {
			if (!hasSameActiveFields(entity, request)) {
				throw new IllegalArgumentException("Only planName and activeThrough can be updated for an active pricing plan");
			}
			if (request.getActiveThrough().isBefore(currentDate)) {
				throw new IllegalArgumentException("activeThrough must be on or after the application current date");
			}
			return;
		}

		if (!hasSamePastFields(entity, request)) {
			throw new IllegalArgumentException("Only planName can be updated for a past pricing plan");
		}
	}

	private boolean isActive(PricingPlanEntity entity, LocalDate currentDate) {
		return !entity.getActiveFrom().isAfter(currentDate) && !entity.getActiveThrough().isBefore(currentDate);
	}

	private boolean hasSameActiveFields(PricingPlanEntity entity, PricingPlanRequest request) {
		return entity.getPlanCode().equals(request.getPlanCode())
				&& entity.getProduct().getId().equals(request.getProductId())
				&& entity.getRegion().getId().equals(request.getRegionId())
				&& entity.getActiveFrom().equals(request.getActiveFrom())
				&& hasSameFees(entity, request);
	}

	private boolean hasSamePastFields(PricingPlanEntity entity, PricingPlanRequest request) {
		return hasSameActiveFields(entity, request)
				&& entity.getActiveThrough().equals(request.getActiveThrough());
	}

	private boolean hasSameFees(PricingPlanEntity entity, PricingPlanRequest request) {
		if (entity.getFees().size() != request.getFees().size()) {
			return false;
		}

		return request.getFees().stream().allMatch(requestFee -> entity.getFees().stream()
				.filter(entityFee -> entityFee.getFee().getId().equals(requestFee.getFeeId()))
				.findFirst()
				.map(entityFee -> entityFee.getAmount().compareTo(requestFee.getAmount()) == 0
						&& entityFee.getReasons().stream().map(EligibilityReasonEntity::getId).collect(Collectors.toSet())
								.equals(new HashSet<>(requestFee.getReasonIds())))
				.orElse(false));
	}
}
