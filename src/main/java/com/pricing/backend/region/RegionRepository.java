package com.pricing.backend.region;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<RegionEntity, Long> {

	List<RegionEntity> findAllByOrderByRegionCodeAsc();
}
