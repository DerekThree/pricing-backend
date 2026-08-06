package com.pricing.backend.fee;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.FeeDetail;
import com.pricing.backend.generated.model.FeeListItem;
import com.pricing.backend.generated.model.FeeRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeService {

	private final FeeRepository feeRepository;
	private final PricingPlanFeeRepository pricingPlanFeeRepository;

	public FeeService(FeeRepository feeRepository, PricingPlanFeeRepository pricingPlanFeeRepository) {
		this.feeRepository = feeRepository;
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
	}

	@Transactional(readOnly = true)
	public List<FeeListItem> list() {
		return feeRepository.findAllByOrderByFeeCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<FeeDetail> get(Long id) {
		return feeRepository.findById(id).map(this::toDetail);
	}

	@Transactional
	public FeeDetail create(FeeRequest request) {
		FeeEntity entity = new FeeEntity();
		apply(entity, request);
		return toDetail(feeRepository.save(entity));
	}

	@Transactional
	public Optional<FeeDetail> update(Long id, FeeRequest request) {
		return feeRepository.findById(id)
				.map(entity -> {
					validateFeeTypeCanChange(entity, request);
					apply(entity, request);
					return toDetail(feeRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!feeRepository.existsById(id)) {
			return false;
		}

		pricingPlanFeeRepository.findFirstByFee_IdOrderByPricingPlan_PlanCodeAsc(id)
				.ifPresent(pricingPlanFee -> {
					throw new RecordInUseException(
							"fee", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
				});

		feeRepository.deleteById(id);
		return true;
	}

	private void validateFeeTypeCanChange(FeeEntity entity, FeeRequest request) {
		if (entity.getFeeType() == request.getFeeType()) {
			return;
		}

		pricingPlanFeeRepository.findFirstByFee_IdOrderByPricingPlan_PlanCodeAsc(entity.getId())
				.ifPresent(pricingPlanFee -> {
					throw new RecordInUseException(
							"fee", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
				});
	}

	private void apply(FeeEntity entity, FeeRequest request) {
		entity.setFeeCode(request.getFeeCode());
		entity.setFeeName(request.getFeeName());
		entity.setFeeType(request.getFeeType());
		entity.setProductTypes(request.getProductTypes());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private FeeListItem toListItem(FeeEntity entity) {
		return new FeeListItem(
				entity.getId(),
				formatCodeAndName(entity.getFeeCode(), entity.getFeeName()),
				entity.getFeeType(),
				entity.getProductTypes(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private FeeDetail toDetail(FeeEntity entity) {
		return new FeeDetail(
				entity.getFeeCode(),
				entity.getFeeName(),
				entity.getFeeType(),
				entity.getProductTypes(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
