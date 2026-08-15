package com.pricing.backend.pricingplan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.pricing.backend.simulator.SimulatorDateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingPlanService {

	private final PricingPlanRepository pricingPlanRepository;
	private final ProductRepository productRepository;
	private final RegionRepository regionRepository;
	private final FeeRepository feeRepository;
	private final EligibilityReasonRepository eligibilityReasonRepository;
	private final SimulatorDateService simulatorDateService;
	private final PricingPlanValidator pricingPlanValidator;
	private final PricingPlanMapper pricingPlanMapper;

	public PricingPlanService(PricingPlanRepository pricingPlanRepository, ProductRepository productRepository,
			RegionRepository regionRepository, FeeRepository feeRepository,
			EligibilityReasonRepository eligibilityReasonRepository, SimulatorDateService simulatorDateService,
			PricingPlanValidator pricingPlanValidator, PricingPlanMapper pricingPlanMapper) {
		this.pricingPlanRepository = pricingPlanRepository;
		this.productRepository = productRepository;
		this.regionRepository = regionRepository;
		this.feeRepository = feeRepository;
		this.eligibilityReasonRepository = eligibilityReasonRepository;
		this.simulatorDateService = simulatorDateService;
		this.pricingPlanValidator = pricingPlanValidator;
		this.pricingPlanMapper = pricingPlanMapper;
	}

	@Transactional(readOnly = true)
	public List<PricingPlanListItem> list() {
		return pricingPlanRepository.findAllByOrderByPlanCodeAsc().stream()
				.map(pricingPlanMapper::toPricingPlanListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<PricingPlanDetail> get(Long id) {
		return pricingPlanRepository.findById(id).map(pricingPlanMapper::toPricingPlanDetail);
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
		PricingPlanEntity entity = new PricingPlanEntity();
		apply(entity, request);
		return pricingPlanMapper.toPricingPlanDetail(pricingPlanRepository.saveAndFlush(entity));
	}

	@Transactional
	public Optional<PricingPlanDetail> update(Long id, PricingPlanRequest request) {
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return pricingPlanMapper.toPricingPlanDetail(pricingPlanRepository.saveAndFlush(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					pricingPlanValidator.validateCanDelete(entity);
					pricingPlanRepository.delete(entity);
					return true;
				})
				.orElse(false);
	}

	private void apply(PricingPlanEntity entity, PricingPlanRequest request) {
		pricingPlanValidator.validate(entity, request);
		ProductEntity product = productRepository.findById(request.getProductId()).orElse(null);
		var region = regionRepository.getReferenceById(request.getRegionId());
		Map<Long, FeeEntity> feesById = feeRepository
				.findAllById(request.getFees().stream().map(PricingPlanFee::getFeeId).toList())
				.stream()
				.collect(Collectors.toMap(FeeEntity::getId, Function.identity()));
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
				pricingPlanFee.getReasons().add(eligibilityReasonRepository.getReferenceById(reasonId));
			}
			entity.getFees().add(pricingPlanFee);
		}
	}

	private PricingPlanOptions buildPricingPlanOptions() {
		return new PricingPlanOptions(
				productRepository.findAllByOrderByProductCodeAsc().stream()
						.map(pricingPlanMapper::toProductOption)
						.toList(),
				regionRepository.findAllByOrderByRegionCodeAsc().stream()
						.map(pricingPlanMapper::toRegionOption)
						.toList())
				.currentDate(simulatorDateService.getCurrentDate())
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
						.map(pricingPlanMapper::toFeeOption)
						.toList())
				.reasons(eligibilityReasonRepository.findAllByOrderByReasonCodeAsc().stream()
						.map(pricingPlanMapper::toReasonOption)
						.toList())
				.intervals(pricingPlanRepository
						.findAllByProductIdAndRegionIdAndActiveThroughGreaterThanEqual(product.getId(), region.getId(), currentDate)
						.stream()
						.filter(plan -> excludedPricingPlanId == null || !plan.getId().equals(excludedPricingPlanId))
						.map(plan -> new PricingPlanInterval(plan.getActiveFrom(), plan.getActiveThrough()))
						.toList());
	}

}
