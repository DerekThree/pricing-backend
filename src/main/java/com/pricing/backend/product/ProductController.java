package com.pricing.backend.product;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.ProductsApi;
import com.pricing.backend.generated.model.ProductDetail;
import com.pricing.backend.generated.model.ProductListItem;
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
	public ResponseEntity<List<ProductListItem>> listProducts() {
		return ResponseEntity.ok(productService.list());
	}

	@Override
	public ResponseEntity<ProductDetail> getProduct(Long id) {
		ProductDetail product = productService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Product", id));

		return ResponseEntity.ok(product);
	}

	@Override
	public ResponseEntity<ProductDetail> createProduct(ProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
	}

	@Override
	public ResponseEntity<ProductDetail> updateProduct(Long id, ProductRequest request) {
		ProductDetail product = productService.update(id, request)
				.orElseThrow(() -> new RecordNotFoundException("Product", id));

		return ResponseEntity.ok(product);
	}

	@Override
	public ResponseEntity<Void> deleteProduct(Long id) {
		if (productService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Product", id);
	}
}
