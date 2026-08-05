package com.pricing.backend.pricingplan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.FeeOption;
import com.pricing.backend.generated.model.PricingPlanDetail;
import com.pricing.backend.generated.model.PricingPlanListItem;
import com.pricing.backend.generated.model.PricingPlanOptions;
import com.pricing.backend.generated.model.PricingPlanRequest;
import com.pricing.backend.generated.model.ProductOption;
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

	public PricingPlanService(PricingPlanRepository pricingPlanRepository, ProductRepository productRepository,
			RegionRepository regionRepository, FeeRepository feeRepository) {
		this.pricingPlanRepository = pricingPlanRepository;
		this.productRepository = productRepository;
		this.regionRepository = regionRepository;
		this.feeRepository = feeRepository;
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
		entity.setPlanCode(request.getPlanCode());
		entity.setPlanName(request.getPlanName());
		entity.setProduct(product);
		entity.setRegion(region);
		entity.setActiveFrom(request.getActiveFrom());
		entity.setActiveThrough(request.getActiveThrough());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
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
				entity.getId(),
				entity.getPlanCode(),
				entity.getPlanName(),
				toProductOption(entity.getProduct()),
				toRegionOption(entity.getRegion()),
				entity.getActiveFrom(),
				entity.getActiveThrough(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
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
				fee.getProductTypes()
		);
	}
}
