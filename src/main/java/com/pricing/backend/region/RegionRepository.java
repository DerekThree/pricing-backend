package com.pricing.backend.region;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<RegionEntity, Long> {

	List<RegionEntity> findAllByOrderByRegionCodeAsc();

	Optional<RegionEntity> findFirstByBranchesContainsOrderByRegionCodeAsc(Long branchId);
}