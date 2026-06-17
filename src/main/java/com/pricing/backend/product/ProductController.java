package com.pricing.backend.product;

import java.util.List;

import com.pricing.backend.generated.api.ProductsApi;
import com.pricing.backend.generated.model.Product;
import com.pricing.backend.generated.model.ProductRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@Override
	public ResponseEntity<List<Product>> listProducts() {
		return ResponseEntity.ok(productService.list());
	}

	@Override
	public ResponseEntity<Product> getProduct(Long id) {
		return productService.get(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Product> createProduct(ProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
	}

	@Override
	public ResponseEntity<Product> updateProduct(Long id, ProductRequest request) {
		return productService.update(id, request)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Void> deleteProduct(Long id) {
		if (productService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.notFound().build();
	}
}
