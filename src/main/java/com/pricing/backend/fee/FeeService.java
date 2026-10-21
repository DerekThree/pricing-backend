package com.pricing.backend.fee;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.FeeDetail;
import com.pricing.backend.generated.model.FeeListItem;
import com.pricing.backend.generated.model.FeeRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeService {

	private static final Logger log = LoggerFactory.getLogger(FeeService.class);

	private final FeeRepository feeRepository;
	private final PricingPlanFeeRepository pricingPlanFeeRepository;
	private final FeeValidator validator;
	private final FeeMapper mapper;

	public FeeService(FeeRepository feeRepository, PricingPlanFeeRepository pricingPlanFeeRepository,
			FeeValidator validator, FeeMapper mapper) {
		this.feeRepository = feeRepository;
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.validator = validator;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<FeeListItem> list() {
		return feeRepository.findAllByOrderByFeeCodeAsc().stream()
				.map(mapper::toFeeListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<FeeDetail> get(Long id) {
		return feeRepository.findById(id).map(mapper::toFeeDetail);
	}

	@Transactional
	public FeeDetail create(FeeRequest request) {
		log.info("Create Fee requested: code={}", request.getFeeCode());
		FeeEntity entity = new FeeEntity();
		apply(entity, request);
		return mapper.toFeeDetail(feeRepository.save(entity));
	}

	@Transactional
	public Optional<FeeDetail> update(Long id, FeeRequest request) {
		log.info("Update Fee requested: id={}", id);
		return feeRepository.findById(id)
				.map(entity -> {
					validator.validateCanUpdate(entity, request);
					apply(entity, request);
					return mapper.toFeeDetail(feeRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		log.info("Delete Fee requested: id={}", id);
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
