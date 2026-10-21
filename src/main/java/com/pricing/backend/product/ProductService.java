package com.pricing.backend.product;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.ProductDetail;
import com.pricing.backend.generated.model.ProductListItem;
import com.pricing.backend.generated.model.ProductRequest;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private static final Logger log = LoggerFactory.getLogger(ProductService.class);

	private final ProductRepository productRepository;
	private final PricingPlanRepository pricingPlanRepository;
	private final ProductMapper mapper;

	public ProductService(ProductRepository productRepository, PricingPlanRepository pricingPlanRepository,
			ProductMapper mapper) {
		this.productRepository = productRepository;
		this.pricingPlanRepository = pricingPlanRepository;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<ProductListItem> list() {
		return productRepository.findAllByOrderByProductCodeAsc().stream()
				.map(mapper::toProductListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<ProductDetail> get(Long id) {
		return productRepository.findById(id).map(mapper::toProductDetail);
	}

	@Transactional
	public ProductDetail create(ProductRequest request) {
		log.info("Create Product requested: code={}", request.getProductCode());
		ProductEntity entity = new ProductEntity();
		apply(entity, request);
		return mapper.toProductDetail(productRepository.save(entity));
	}

	@Transactional
	public Optional<ProductDetail> update(Long id, ProductRequest request) {
		log.info("Update Product requested: id={}", id);
		return productRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return mapper.toProductDetail(productRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		log.info("Delete Product requested: id={}", id);
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

}
