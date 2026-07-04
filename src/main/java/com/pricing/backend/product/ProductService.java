package com.pricing.backend.product;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.Product;
import com.pricing.backend.generated.model.ProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<Product> list() {
		return productRepository.findAllByOrderByProductCodeAsc().stream()
				.map(this::toModel)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<Product> get(Long id) {
		return productRepository.findById(id).map(this::toModel);
	}

	@Transactional
	public Product create(ProductRequest request) {
		ProductEntity entity = new ProductEntity();
		apply(entity, request);
		return toModel(productRepository.save(entity));
	}

	@Transactional
	public Optional<Product> update(Long id, ProductRequest request) {
		return productRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toModel(productRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!productRepository.existsById(id)) {
			return false;
		}

		productRepository.deleteById(id);
		return true;
	}

	private void apply(ProductEntity entity, ProductRequest request) {
		entity.setProductCode(request.getProductCode());
		entity.setProductName(request.getProductName());
		entity.setAccountType(request.getAccountType());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private Product toModel(ProductEntity entity) {
		return new Product(
				entity.getId(),
				entity.getProductCode(),
				entity.getProductName(),
				entity.getAccountType(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}
}
