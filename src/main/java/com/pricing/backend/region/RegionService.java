package com.pricing.backend.region;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.branch.BranchService;
import com.pricing.backend.generated.model.Region;
import com.pricing.backend.generated.model.RegionBranchOption;
import com.pricing.backend.generated.model.RegionOptions;
import com.pricing.backend.generated.model.RegionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionService {

	private final RegionRepository regionRepository;
	private final BranchService branchService;

	public RegionService(RegionRepository regionRepository, BranchService branchService) {
		this.regionRepository = regionRepository;
		this.branchService = branchService;
	}

	@Transactional(readOnly = true)
	public List<Region> list() {
		return regionRepository.findAllByOrderByRegionCodeAsc().stream()
				.map(this::toModel)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<Region> get(Long id) {
		return regionRepository.findById(id).map(this::toModel);
	}

	@Transactional(readOnly = true)
	public RegionOptions options() {
		return new RegionOptions(
				list().stream().flatMap(region -> region.getStates().stream()).distinct().sorted().toList(),
				list().stream().flatMap(region -> region.getZipCodes().stream()).distinct().sorted().toList(),
				branchService.list().stream()
						.map(branch -> new RegionBranchOption(branch.getBranchCode(), branch.getBranchName()))
						.toList()
		);
	}

	@Transactional
	public Region create(RegionRequest request) {
		RegionEntity entity = new RegionEntity();
		apply(entity, request);
		return toModel(regionRepository.save(entity));
	}

	@Transactional
	public Optional<Region> update(Long id, RegionRequest request) {
		return regionRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toModel(regionRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!regionRepository.existsById(id)) {
			return false;
		}

		regionRepository.deleteById(id);
		return true;
	}

	private void apply(RegionEntity entity, RegionRequest request) {
		entity.setRegionCode(request.getRegionCode());
		entity.setRegionName(request.getRegionName());
		entity.setStates(new LinkedHashSet<>(request.getStates()));
		entity.setZipCodes(new LinkedHashSet<>(request.getZipCodes()));
		entity.setBranches(new LinkedHashSet<>(request.getBranches()));
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private Region toModel(RegionEntity entity) {
		return new Region(
				entity.getId(),
				entity.getRegionCode(),
				entity.getRegionName(),
				entity.getStates(),
				entity.getZipCodes(),
				entity.getBranches(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}
}
