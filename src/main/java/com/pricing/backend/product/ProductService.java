package com.pricing.backend.product;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.pricing.backend.generated.model.AccountType;
import com.pricing.backend.generated.model.Product;
import com.pricing.backend.generated.model.ProductRequest;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

	private final Map<Long, Product> products = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(3);

	public ProductService() {
		products.put(1L, new Product(
				1L,
				"1FC00012",
				"Free Checking",
				AccountType.DEPOSIT,
				OffsetDateTime.parse("2026-06-06T10:00:00+08:00"),
				"Derek Ochal"
		));
		products.put(2L, new Product(
				2L,
				"1PL00056",
				"Personal Loan",
				AccountType.CREDIT,
				OffsetDateTime.parse("2026-06-06T10:15:00+08:00"),
				"John Smith"
		));
	}

	public List<Product> list() {
		return products.values().stream()
				.sorted(Comparator.comparing(Product::getProductCode))
				.toList();
	}

	public Optional<Product> get(Long id) {
		return Optional.ofNullable(products.get(id));
	}

	public Product create(ProductRequest request) {
		Product product = new Product(
				nextId.getAndIncrement(),
				request.getProductCode(),
				request.getProductName(),
				request.getAccountType(),
				now(),
				request.getUpdatedBy()
		);
		products.put(product.getId(), product);
		return product;
	}

	public Optional<Product> update(Long id, ProductRequest request) {
		Optional<Product> existingProduct = get(id);
		if (existingProduct.isEmpty()) {
			return Optional.empty();
		}

		Product product = new Product(
				id,
				request.getProductCode(),
				request.getProductName(),
				request.getAccountType(),
				now(),
				request.getUpdatedBy()
		);
		products.put(id, product);
		return Optional.of(product);
	}

	public boolean delete(Long id) {
		return products.remove(id) != null;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now();
	}
}
