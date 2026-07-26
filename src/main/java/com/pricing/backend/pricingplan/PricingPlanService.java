package com.pricing.backend.pricingplan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.PricingPlanDetail;
import com.pricing.backend.generated.model.PricingPlanListItem;
import com.pricing.backend.generated.model.PricingPlanRequest;
import com.pricing.backend.generated.model.ProductRegionOptions;
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

	public PricingPlanService(PricingPlanRepository pricingPlanRepository, ProductRepository productRepository,
			RegionRepository regionRepository) {
		this.pricingPlanRepository = pricingPlanRepository;
		this.productRepository = productRepository;
		this.regionRepository = regionRepository;
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
	public ProductRegionOptions getOptions() {
		return buildProductRegionOptions();
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
		ProductEntity product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new IllegalArgumentException("Product with id " + request.getProductId() + " was not found"));
		RegionEntity region = regionRepository.findById(request.getRegionId())
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
		ProductOption product = new ProductOption(entity.getProduct().getId(), entity.getProduct().getProductCode(), entity.getProduct().getProductName());
		RegionOption region = new RegionOption(entity.getRegion().getId(), entity.getRegion().getRegionCode(), entity.getRegion().getRegionName());
		return new PricingPlanDetail(
				entity.getId(),
				entity.getPlanCode(),
				entity.getPlanName(),
				product.getId(),
				region.getId(),
				new ProductRegionOptions(List.of(product), List.of(region)),
				entity.getActiveFrom(),
				entity.getActiveThrough(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}

	private ProductRegionOptions buildProductRegionOptions() {
		return new ProductRegionOptions(
				productRepository.findAllByOrderByProductCodeAsc().stream()
						.map(product -> new ProductOption(product.getId(), product.getProductCode(), product.getProductName()))
						.toList(),
				regionRepository.findAllByOrderByRegionCodeAsc().stream()
						.map(region -> new RegionOption(region.getId(), region.getRegionCode(), region.getRegionName()))
						.toList()
		);
	}
}
