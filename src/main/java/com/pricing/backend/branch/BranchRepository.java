package com.pricing.backend.branch;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<BranchEntity, Long> {

	List<BranchEntity> findAllByOrderByBranchCodeAsc();
}
