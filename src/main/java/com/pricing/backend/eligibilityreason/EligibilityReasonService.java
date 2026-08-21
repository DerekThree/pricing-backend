package com.pricing.backend.eligibilityreason;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.ReasonCondition;
import com.pricing.backend.generated.model.ReasonDetail;
import com.pricing.backend.generated.model.ReasonListItem;
import com.pricing.backend.generated.model.ReasonOptions;
import com.pricing.backend.generated.model.ReasonRequest;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibilityReasonService {

	private final EligibilityReasonRepository eligibilityReasonRepository;
	private final AccountAttributeRepository accountAttributeRepository;
	private final PricingPlanFeeRepository pricingPlanFeeRepository;
	private final EligibilityReasonValidator eligibilityReasonValidator;
	private final EligibilityReasonNormalizer eligibilityReasonNormalizer;
	private final EligibilityReasonMapper eligibilityReasonMapper;

	public EligibilityReasonService(EligibilityReasonRepository eligibilityReasonRepository,
			AccountAttributeRepository accountAttributeRepository, PricingPlanFeeRepository pricingPlanFeeRepository,
			EligibilityReasonValidator eligibilityReasonValidator,
			EligibilityReasonNormalizer eEligibilityReasonNormalizer,
			EligibilityReasonMapper eligibilityReasonMapper) {
		this.eligibilityReasonRepository = eligibilityReasonRepository;
		this.accountAttributeRepository = accountAttributeRepository;
		this.pricingPlanFeeRepository = pricingPlanFeeRepository;
		this.eligibilityReasonValidator = eligibilityReasonValidator;
		this.eligibilityReasonNormalizer = eEligibilityReasonNormalizer;
		this.eligibilityReasonMapper = eligibilityReasonMapper;
	}

	@Transactional(readOnly = true)
	public List<ReasonListItem> list() {
		return eligibilityReasonRepository.findAllByOrderByReasonCodeAsc().stream()
				.map(eligibilityReasonMapper::toReasonListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<ReasonDetail> get(Long id) {
		return eligibilityReasonRepository.findById(id).map(eligibilityReasonMapper::toReasonDetail);
	}

	@Transactional(readOnly = true)
	public ReasonOptions getOptions() {
		return buildOptions();
	}

	@Transactional
	public ReasonDetail create(ReasonRequest request) {
		EligibilityReasonEntity entity = new EligibilityReasonEntity();
		apply(entity, request);
		return eligibilityReasonMapper.toReasonDetail(eligibilityReasonRepository.save(entity));
	}

	@Transactional
	public Optional<ReasonDetail> update(Long id, ReasonRequest request) {
		return eligibilityReasonRepository.findById(id)
				.map(entity -> {
					eligibilityReasonValidator.validateCanUpdate(entity, request);
					apply(entity, request);
					return eligibilityReasonMapper.toReasonDetail(eligibilityReasonRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!eligibilityReasonRepository.existsById(id)) {
			return false;
		}

		pricingPlanFeeRepository.findFirstByReasons_IdOrderByPricingPlan_PlanCodeAsc(id)
				.ifPresent(pricingPlanFee -> {
					throw new RecordInUseException(
							"eligibility reason", "pricing plan", pricingPlanFee.getPricingPlan().getPlanCode());
				});

		eligibilityReasonRepository.deleteById(id);
		return true;
	}

	private void apply(EligibilityReasonEntity entity, ReasonRequest request) {
		Map<Long, AccountAttributeEntity> attributesById = accountAttributeRepository
				.findAllById(request.getConditions().stream().map(ReasonCondition::getAttributeId).toList())
				.stream()
				.collect(Collectors.toMap(AccountAttributeEntity::getId, Function.identity()));
		eligibilityReasonValidator.validateProductTypes(attributesById.values());
		entity.setReasonCode(request.getReasonCode());
		entity.setReasonName(request.getReasonName());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
		entity.getConditions().clear();
		for (ReasonCondition condition : request.getConditions()) {
			AccountAttributeEntity attribute = attributesById.get(condition.getAttributeId());

			entity.getConditions().add(new EligibilityReasonConditionEntity(
					null,
					entity,
					accountAttributeRepository.getReferenceById(condition.getAttributeId()),
					condition.getOperator().getValue(),
					attribute == null ? eligibilityReasonNormalizer.serializeScalarValue(condition.getValue())
							: eligibilityReasonNormalizer.normalizeAttributeValue(attribute, condition.getValue())
			));
		}
	}

	private ReasonOptions buildOptions() {
		return new ReasonOptions(
				accountAttributeRepository.findAllByOrderByAttributeCodeAsc().stream()
						.map(eligibilityReasonMapper::toAttributeOption)
						.toList()
		);
	}

}
