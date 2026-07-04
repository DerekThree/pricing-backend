package com.pricing.backend.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	List<ProductEntity> findAllByOrderByProductCodeAsc();
}
