package com.pricing.backend.product;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.ProductDetail;
import com.pricing.backend.generated.model.ProductListItem;
import com.pricing.backend.generated.model.ProductRequest;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final PricingPlanRepository pricingPlanRepository;

	public ProductService(ProductRepository productRepository, PricingPlanRepository pricingPlanRepository) {
		this.productRepository = productRepository;
		this.pricingPlanRepository = pricingPlanRepository;
	}

	@Transactional(readOnly = true)
	public List<ProductListItem> list() {
		return productRepository.findAllByOrderByProductCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<ProductDetail> get(Long id) {
		return productRepository.findById(id).map(this::toDetail);
	}

	@Transactional
	public ProductDetail create(ProductRequest request) {
		ProductEntity entity = new ProductEntity();
		apply(entity, request);
		return toDetail(productRepository.save(entity));
	}

	@Transactional
	public Optional<ProductDetail> update(Long id, ProductRequest request) {
		return productRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(productRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!productRepository.existsById(id)) {
			return false;
		}

		pricingPlanRepository.findFirstByProductIdOrderByPlanCodeAsc(id)
				.ifPresent(pricingPlan -> {
					throw new RecordInUseException("product", "pricing plan", pricingPlan.getPlanCode());
				});

		productRepository.deleteById(id);
		return true;
	}

	private void apply(ProductEntity entity, ProductRequest request) {
		entity.setProductCode(request.getProductCode());
		entity.setProductName(request.getProductName());
		entity.setProductType(request.getProductType());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private ProductListItem toListItem(ProductEntity entity) {
		return new ProductListItem(
				entity.getId(),
				formatCodeAndName(entity.getProductCode(), entity.getProductName()),
				entity.getProductType(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private ProductDetail toDetail(ProductEntity entity) {
		return new ProductDetail(
				entity.getProductCode(),
				entity.getProductName(),
				entity.getProductType(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
