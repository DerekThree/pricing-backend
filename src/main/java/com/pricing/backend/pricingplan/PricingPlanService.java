package com.pricing.backend.pricingplan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.model.PricingPlanDetail;
import com.pricing.backend.generated.model.PricingPlanFee;
import com.pricing.backend.generated.model.PricingPlanInterval;
import com.pricing.backend.generated.model.PricingPlanListItem;
import com.pricing.backend.generated.model.PricingPlanOptions;
import com.pricing.backend.generated.model.PricingPlanRequest;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.product.ProductRepository;
import com.pricing.backend.region.RegionEntity;
import com.pricing.backend.region.RegionRepository;
import com.pricing.backend.simulator.SimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingPlanService {

	private static final Logger log = LoggerFactory.getLogger(PricingPlanService.class);

	private final PricingPlanRepository pricingPlanRepository;
	private final ProductRepository productRepository;
	private final RegionRepository regionRepository;
	private final FeeRepository feeRepository;
	private final EligibilityReasonRepository eligibilityReasonRepository;
	private final SimulatorService simulatorService;
	private final PricingPlanValidator validator;
	private final PricingPlanMapper mapper;

	public PricingPlanService(PricingPlanRepository pricingPlanRepository, ProductRepository productRepository,
			RegionRepository regionRepository, FeeRepository feeRepository,
			EligibilityReasonRepository eligibilityReasonRepository, SimulatorService simulatorService,
			PricingPlanValidator validator, PricingPlanMapper mapper) {
		this.pricingPlanRepository = pricingPlanRepository;
		this.productRepository = productRepository;
		this.regionRepository = regionRepository;
		this.feeRepository = feeRepository;
		this.eligibilityReasonRepository = eligibilityReasonRepository;
		this.simulatorService = simulatorService;
		this.validator = validator;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<PricingPlanListItem> list() {
		return pricingPlanRepository.findAllByOrderByPlanCodeAsc().stream()
				.map(mapper::toPricingPlanListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<PricingPlanDetail> get(Long id) {
		return pricingPlanRepository.findById(id).map(mapper::toPricingPlanDetail);
	}

	@Transactional(readOnly = true)
	public PricingPlanOptions getOptions(Long recordId) {
		PricingPlanOptions options = buildPricingPlanOptions();
		if (recordId == null) {
			return options;
		}

		PricingPlanEntity record = pricingPlanRepository.findById(recordId)
				.orElseThrow(() -> new RecordNotFoundException("Pricing plan", recordId));
		return addSecondaryOptions(options, record.getProduct(), record.getRegion(), record.getId());
	}

	@Transactional(readOnly = true)
	public PricingPlanOptions getSecondaryOptions(Long productId, Long regionId) {
		ProductEntity product = productRepository.findById(productId)
				.orElseThrow(() -> new RecordNotFoundException("Product", productId));
		RegionEntity region = regionRepository.findById(regionId)
				.orElseThrow(() -> new RecordNotFoundException("Region", regionId));
		return addSecondaryOptions(buildPricingPlanOptions(), product, region, null);
	}

	@Transactional
	public PricingPlanDetail create(PricingPlanRequest request) {
		log.info("Create Pricing Plan requested: code={}", request.getPlanCode());
		PricingPlanEntity entity = new PricingPlanEntity();
		apply(entity, request);
		return mapper.toPricingPlanDetail(pricingPlanRepository.saveAndFlush(entity));
	}

	@Transactional
	public Optional<PricingPlanDetail> update(Long id, PricingPlanRequest request) {
		log.info("Update Pricing Plan requested: id={}", id);
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return mapper.toPricingPlanDetail(pricingPlanRepository.saveAndFlush(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		log.info("Delete Pricing Plan requested: id={}", id);
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					validator.validateCanDelete(entity);
					pricingPlanRepository.delete(entity);
					return true;
				})
				.orElse(false);
	}

	private void apply(PricingPlanEntity entity, PricingPlanRequest request) {
		validator.validate(entity, request);
		ProductEntity product = productRepository.findById(request.getProductId()).orElse(null);
		var region = regionRepository.getReferenceById(request.getRegionId());
		Map<Long, FeeEntity> feesById = feeRepository
				.findAllById(request.getFees().stream().map(PricingPlanFee::getFeeId).toList())
				.stream()
				.collect(Collectors.toMap(FeeEntity::getId, Function.identity()));
		Map<Long, EligibilityReasonEntity> reasonsById = eligibilityReasonRepository
				.findAllById(request.getFees().stream()
						.flatMap(fee -> fee.getReasonIds().stream())
						.toList())
				.stream()
				.collect(Collectors.toMap(EligibilityReasonEntity::getId, Function.identity()));
		entity.setPlanCode(request.getPlanCode());
		entity.setPlanName(request.getPlanName());
		entity.setProduct(productRepository.getReferenceById(request.getProductId()));
		entity.setRegion(region);
		entity.setActiveFrom(request.getActiveFrom());
		entity.setActiveThrough(request.getActiveThrough());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
		entity.getFees().clear();
		for (PricingPlanFee feeRequest : request.getFees()) {
			FeeEntity fee = feesById.get(feeRequest.getFeeId());
			if (product != null && fee != null && !fee.getProductTypes().contains(product.getProductType())) {
				throw new IllegalArgumentException("Fee with code " + fee.getFeeCode() + " cannot be used for this product type");
			}

			PricingPlanFeeEntity pricingPlanFee = new PricingPlanFeeEntity();
			pricingPlanFee.setId(new PricingPlanFeeId(entity.getId(), feeRequest.getFeeId()));
			pricingPlanFee.setPricingPlan(entity);
			pricingPlanFee.setFee(feeRepository.getReferenceById(feeRequest.getFeeId()));
			pricingPlanFee.setAmount(feeRequest.getAmount());
			for (Long reasonId : feeRequest.getReasonIds()) {
				EligibilityReasonEntity reason = reasonsById.get(reasonId);
				if (product != null && reason != null) {
					validator.validateReasonProductType(product.getProductType(), reason);
				}
				pricingPlanFee.getReasons().add(eligibilityReasonRepository.getReferenceById(reasonId));
			}
			entity.getFees().add(pricingPlanFee);
		}
	}

	private PricingPlanOptions buildPricingPlanOptions() {
		return new PricingPlanOptions(
				productRepository.findAllByOrderByProductCodeAsc().stream()
						.map(mapper::toProductOption)
						.toList(),
				regionRepository.findAllByOrderByRegionCodeAsc().stream()
						.map(mapper::toRegionOption)
						.toList())
				.currentDate(simulatorService.getCurrentDate())
				.fees(null)
				.reasons(null)
				.intervals(null);
	}

	private PricingPlanOptions addSecondaryOptions(PricingPlanOptions options, ProductEntity product,
			RegionEntity region, Long excludedPricingPlanId) {
		LocalDate currentDate = options.getCurrentDate();
		return options
				.productId(product.getId())
				.regionId(region.getId())
				.fees(feeRepository.findAllByOrderByFeeCodeAsc().stream()
						.filter(fee -> fee.getProductTypes().contains(product.getProductType()))
						.map(mapper::toFeeOption)
						.toList())
				.reasons(eligibilityReasonRepository.findAllByOrderByReasonCodeAsc().stream()
						.map(mapper::toReasonOption)
						.toList())
				.intervals(pricingPlanRepository
						.findAllByProductIdAndRegionIdAndActiveThroughGreaterThanEqual(product.getId(), region.getId(), currentDate)
						.stream()
						.filter(plan -> excludedPricingPlanId == null || !plan.getId().equals(excludedPricingPlanId))
						.map(plan -> new PricingPlanInterval(plan.getActiveFrom(), plan.getActiveThrough()))
						.toList());
	}

}
