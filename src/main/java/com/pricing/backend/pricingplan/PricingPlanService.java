package com.pricing.backend.pricingplan;

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
import com.pricing.backend.generated.model.FeeOption;
import com.pricing.backend.generated.model.PricingPlanDetail;
import com.pricing.backend.generated.model.PricingPlanFeeRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingPlanService {

	private final PricingPlanRepository pricingPlanRepository;
	private final ProductRepository productRepository;
	private final RegionRepository regionRepository;
	private final FeeRepository feeRepository;
	private final EligibilityReasonRepository eligibilityReasonRepository;

	public PricingPlanService(PricingPlanRepository pricingPlanRepository, ProductRepository productRepository,
			RegionRepository regionRepository, FeeRepository feeRepository,
			EligibilityReasonRepository eligibilityReasonRepository) {
		this.pricingPlanRepository = pricingPlanRepository;
		this.productRepository = productRepository;
		this.regionRepository = regionRepository;
		this.feeRepository = feeRepository;
		this.eligibilityReasonRepository = eligibilityReasonRepository;
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
	public PricingPlanOptions getOptions() {
		return buildPricingPlanOptions();
	}

	@Transactional
	public PricingPlanDetail create(PricingPlanRequest request) {
		PricingPlanEntity entity = new PricingPlanEntity();
		apply(entity, request);
		return toDetail(pricingPlanRepository.save(entity));
	}

	@Transactional
	public Optional<PricingPlanDetail> update(Long id, PricingPlanRequest request) {
		return pricingPlanRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(pricingPlanRepository.save(entity));
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
		var product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new IllegalArgumentException("Product with id " + request.getProductId() + " was not found"));
		var region = regionRepository.findById(request.getRegionId())
				.orElseThrow(() -> new IllegalArgumentException("Region with id " + request.getRegionId() + " was not found"));
		if (request.getActiveFrom().isAfter(request.getActiveThrough())) {
			throw new IllegalArgumentException("activeFrom must be on or before activeThrough");
		}
		Map<Long, FeeEntity> feesById = feeRepository
				.findAllById(request.getFees().stream().map(PricingPlanFeeRequest::getFeeId).toList())
				.stream()
				.collect(Collectors.toMap(FeeEntity::getId, Function.identity()));
		Map<Long, EligibilityReasonEntity> reasonsById = eligibilityReasonRepository
				.findAllById(request.getFees().stream().flatMap(fee -> fee.getReasonIds().stream()).toList())
				.stream()
				.collect(Collectors.toMap(EligibilityReasonEntity::getId, Function.identity()));
		Set<Long> uniqueFeeIds = new HashSet<>();
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
			if (!uniqueFeeIds.add(fee.getId())) {
				throw new IllegalArgumentException("Duplicate fee with id " + fee.getId());
			}
			if (!fee.getProductTypes().contains(product.getProductType())) {
				throw new IllegalArgumentException("Fee with id " + fee.getId() + " cannot be used for this product type");
			}

			PricingPlanFeeEntity pricingPlanFee = new PricingPlanFeeEntity();
			pricingPlanFee.setId(new PricingPlanFeeId(entity.getId(), fee.getId()));
			pricingPlanFee.setPricingPlan(entity);
			pricingPlanFee.setFee(fee);
			pricingPlanFee.setAmount(feeRequest.getAmount());
			Set<Long> uniqueReasonIds = new HashSet<>();
			for (Long reasonId : feeRequest.getReasonIds()) {
				EligibilityReasonEntity reason = reasonsById.get(reasonId);
				if (reason == null) {
					throw new IllegalArgumentException("Eligibility reason with id " + reasonId + " was not found");
				}
				if (!uniqueReasonIds.add(reason.getId())) {
					throw new IllegalArgumentException("Duplicate eligibility reason with id " + reason.getId());
				}
				pricingPlanFee.getReasons().add(reason);
			}
			entity.getFees().add(pricingPlanFee);
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
				new PricingPlanOptions(
						List.of(toProductOption(entity.getProduct())),
						List.of(toRegionOption(entity.getRegion())),
						entity.getFees().stream()
								.sorted(pricingPlanFeeComparator())
								.map(PricingPlanFeeEntity::getFee)
								.map(this::toFeeOption)
								.toList(),
						entity.getFees().stream()
								.flatMap(fee -> fee.getReasons().stream())
								.distinct()
								.sorted(reasonComparator())
								.map(this::toReasonOption)
								.toList()
				)
		);
	}

	private PricingPlanFeeRequest toPricingPlanFeeDetail(PricingPlanFeeEntity entity) {
		return new PricingPlanFeeRequest(entity.getFee().getId(), entity.getAmount())
				.reasonIds(entity.getReasons().stream()
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
						.toList(),
				feeRepository.findAllByOrderByFeeCodeAsc().stream()
						.map(this::toFeeOption)
						.toList(),
				eligibilityReasonRepository.findAllByOrderByReasonCodeAsc().stream()
						.map(this::toReasonOption)
						.toList()
		);
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
