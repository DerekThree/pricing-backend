package com.pricing.backend.pricingplan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
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
import com.pricing.backend.generated.model.FeeOption;
import com.pricing.backend.generated.model.PricingPlanDetail;
import com.pricing.backend.generated.model.PricingPlanFeeRequest;
import com.pricing.backend.generated.model.PricingPlanInterval;
import com.pricing.backend.generated.model.PricingPlanListItem;
import com.pricing.backend.generated.model.PricingPlanOptions;
import com.pricing.backend.generated.model.PricingPlanRequest;
import com.pricing.backend.generated.model.ProductOption;
import com.pricing.backend.generated.model.ReasonOption;
import com.pricing.backend.generated.model.RegionOption;
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

	public PricingPlanService(PricingPlanRepository pricingPlanRepository, ProductRepository productRepository,
			RegionRepository regionRepository, FeeRepository feeRepository,
			EligibilityReasonRepository eligibilityReasonRepository, SimulatorDateService simulatorDateService) {
		this.pricingPlanRepository = pricingPlanRepository;
		this.productRepository = productRepository;
		this.regionRepository = regionRepository;
		this.feeRepository = feeRepository;
		this.eligibilityReasonRepository = eligibilityReasonRepository;
		this.simulatorDateService = simulatorDateService;
	}

	@Transactional(readOnly = true)
	public List<PricingPlanListItem> list() {
		return pricingPlanRepository.findAllByOrderByPlanCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<PricingPlanDetail> get(Long id) {
		return pricingPlanRepository.findById(id).map(this::toDetail);
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
		return toDetail(pricingPlanRepository.saveAndFlush(entity));
	}

	@Transactional
	public Optional<PricingPlanDetail> update(Long id, PricingPlanRequest request) {
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(pricingPlanRepository.saveAndFlush(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					if (isActive(entity, simulatorDateService.getCurrentDate())) {
						throw new IllegalArgumentException("Active pricing plans cannot be deleted");
					}

					pricingPlanRepository.delete(entity);
					return true;
				})
				.orElse(false);
	}

	private void apply(PricingPlanEntity entity, PricingPlanRequest request) {
		validateLifecycle(entity, request);
		var product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new IllegalArgumentException("Product with id " + request.getProductId() + " was not found"));
		var region = regionRepository.getReferenceById(request.getRegionId());
		validateActivePeriod(entity, request);
		Map<Long, FeeEntity> feesById = feeRepository
				.findAllById(request.getFees().stream().map(PricingPlanFeeRequest::getFeeId).toList())
				.stream()
				.collect(Collectors.toMap(FeeEntity::getId, Function.identity()));
		entity.setPlanCode(request.getPlanCode());
		entity.setPlanName(request.getPlanName());
		entity.setProduct(product);
		entity.setRegion(region);
		entity.setActiveFrom(request.getActiveFrom());
		entity.setActiveThrough(request.getActiveThrough());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
		entity.getFees().clear();
		for (PricingPlanFeeRequest feeRequest : request.getFees()) {
			FeeEntity fee = feesById.get(feeRequest.getFeeId());
			if (fee == null) {
				throw new IllegalArgumentException("Fee with id " + feeRequest.getFeeId() + " was not found");
			}
			if (!fee.getProductTypes().contains(product.getProductType())) {
				throw new IllegalArgumentException("Fee with id " + fee.getId() + " cannot be used for this product type");
			}

			PricingPlanFeeEntity pricingPlanFee = new PricingPlanFeeEntity();
			pricingPlanFee.setId(new PricingPlanFeeId(entity.getId(), fee.getId()));
			pricingPlanFee.setPricingPlan(entity);
			pricingPlanFee.setFee(fee);
			pricingPlanFee.setAmount(feeRequest.getAmount());
			for (Long reasonId : feeRequest.getReasonIds()) {
				pricingPlanFee.getReasons().add(eligibilityReasonRepository.getReferenceById(reasonId));
			}
			entity.getFees().add(pricingPlanFee);
		}
	}

	private void validateLifecycle(PricingPlanEntity entity, PricingPlanRequest request) {
		LocalDate currentDate = simulatorDateService.getCurrentDate();
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

	private void validateActivePeriod(PricingPlanEntity entity, PricingPlanRequest request) {
		if (entity.getId() == null &&
				request.getActiveFrom().isBefore(simulatorDateService.getCurrentDate())) {
			throw new IllegalArgumentException("activeFrom must be on or after the application current date");
		}
	}

	private PricingPlanListItem toListItem(PricingPlanEntity entity) {
		return new PricingPlanListItem(
				entity.getId(),
				formatCodeAndName(entity.getPlanCode(), entity.getPlanName()),
				formatCodeAndName(entity.getProduct().getProductCode(), entity.getProduct().getProductName()),
				formatCodeAndName(entity.getRegion().getRegionCode(), entity.getRegion().getRegionName()),
				entity.getActiveFrom(),
				entity.getActiveThrough(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private PricingPlanDetail toDetail(PricingPlanEntity entity) {
		return new PricingPlanDetail(
				entity.getPlanCode(),
				entity.getPlanName(),
				entity.getProduct().getId(),
				entity.getRegion().getId(),
				entity.getActiveFrom(),
				entity.getActiveThrough(),
				entity.getFees().stream()
						.sorted(pricingPlanFeeComparator())
						.map(this::toPricingPlanFeeDetail)
						.toList(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn(),
				new PricingPlanOptions(List.of(toProductOption(entity.getProduct())),
						List.of(toRegionOption(entity.getRegion())))
						.fees(entity.getFees().stream()
								.sorted(pricingPlanFeeComparator())
								.map(PricingPlanFeeEntity::getFee)
								.map(this::toFeeOption)
								.toList())
						.reasons(entity.getFees().stream()
								.flatMap(fee -> fee.getReasons().stream())
								.distinct()
								.sorted(reasonComparator())
								.map(this::toReasonOption)
								.toList())
						.intervals(null)
		);
	}

	private PricingPlanFeeRequest toPricingPlanFeeDetail(PricingPlanFeeEntity entity) {
		return new PricingPlanFeeRequest(
				entity.getFee().getId(), entity.getAmount(), entity.getReasons().stream()
						.sorted(reasonComparator())
						.map(reason -> reason.getId())
						.toList());
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}

	private PricingPlanOptions buildPricingPlanOptions() {
		return new PricingPlanOptions(
				productRepository.findAllByOrderByProductCodeAsc().stream()
						.map(this::toProductOption)
						.toList(),
				regionRepository.findAllByOrderByRegionCodeAsc().stream()
						.map(this::toRegionOption)
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
						.map(this::toFeeOption)
						.toList())
				.reasons(eligibilityReasonRepository.findAllByOrderByReasonCodeAsc().stream()
						.map(this::toReasonOption)
						.toList())
				.intervals(pricingPlanRepository
						.findAllByProductIdAndRegionIdAndActiveThroughGreaterThanEqual(product.getId(), region.getId(), currentDate)
						.stream()
						.filter(plan -> excludedPricingPlanId == null || !plan.getId().equals(excludedPricingPlanId))
						.map(plan -> new PricingPlanInterval(plan.getActiveFrom(), plan.getActiveThrough()))
						.toList());
	}

	private ProductOption toProductOption(ProductEntity product) {
		return new ProductOption(
				product.getId(),
				product.getProductCode(),
				product.getProductName(),
				product.getProductType()
		);
	}

	private RegionOption toRegionOption(RegionEntity region) {
		return new RegionOption(
				region.getId(), 
				region.getRegionCode(), 
				region.getRegionName()
		);
	}

	private FeeOption toFeeOption(FeeEntity fee) {
		return new FeeOption(
				fee.getId(),
				fee.getFeeCode(),
				fee.getFeeName(),
				fee.getFeeType(),
				fee.getProductTypes()
		);
	}

	private ReasonOption toReasonOption(EligibilityReasonEntity reason) {
		return new ReasonOption(
				reason.getId(),
				reason.getReasonCode(),
				reason.getReasonName()
		);
	}

	private Comparator<PricingPlanFeeEntity> pricingPlanFeeComparator() {
		return Comparator.comparing((PricingPlanFeeEntity fee) -> fee.getFee().getFeeCode());
	}

	private Comparator<EligibilityReasonEntity> reasonComparator() {
		return Comparator.comparing(EligibilityReasonEntity::getReasonCode);
	}
}
