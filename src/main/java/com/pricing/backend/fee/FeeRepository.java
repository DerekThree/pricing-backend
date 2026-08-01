package com.pricing.backend.fee;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeRepository extends JpaRepository<FeeEntity, Long> {

	List<FeeEntity> findAllByOrderByFeeCodeAsc();
}
