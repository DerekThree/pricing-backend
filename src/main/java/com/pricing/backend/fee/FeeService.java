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
	private final FeeValidator feeValidator;
	private final FeeMapper feeMapper;

	public FeeService(FeeRepository feeRepository, PricingPlanFeeRepository pricingPlanFeeRepository,
			FeeValidator feeValidator, FeeMapper feeMapper) {
		this.feeRepository = feeRepository;
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.feeValidator = feeValidator;
		this.feeMapper = feeMapper;
	}

	@Transactional(readOnly = true)
	public List<FeeListItem> list() {
		return feeRepository.findAllByOrderByFeeCodeAsc().stream()
				.map(feeMapper::toFeeListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<FeeDetail> get(Long id) {
		return feeRepository.findById(id).map(feeMapper::toFeeDetail);
	}

	@Transactional
	public FeeDetail create(FeeRequest request) {
		FeeEntity entity = new FeeEntity();
		apply(entity, request);
		return feeMapper.toFeeDetail(feeRepository.save(entity));
	}

	@Transactional
	public Optional<FeeDetail> update(Long id, FeeRequest request) {
		return feeRepository.findById(id)
				.map(entity -> {
					feeValidator.validateFeeTypeCanChange(entity, request);
					apply(entity, request);
					return feeMapper.toFeeDetail(feeRepository.save(entity));
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

	private void apply(FeeEntity entity, FeeRequest request) {
		entity.setFeeCode(request.getFeeCode());
		entity.setFeeName(request.getFeeName());
		entity.setFeeType(request.getFeeType());
		entity.setProductTypes(request.getProductTypes());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

}
