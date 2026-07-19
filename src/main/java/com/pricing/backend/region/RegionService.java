package com.pricing.backend.region;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.generated.model.BranchOption;
import com.pricing.backend.generated.model.CoverageOptions;
import com.pricing.backend.generated.model.RegionDetail;
import com.pricing.backend.generated.model.RegionListItem;
import com.pricing.backend.generated.model.RegionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionService {

	private final RegionRepository regionRepository;
	private final BranchRepository branchRepository;

	public RegionService(RegionRepository regionRepository, BranchRepository branchRepository) {
		this.regionRepository = regionRepository;
		this.branchRepository = branchRepository;
	}

	@Transactional(readOnly = true)
	public List<RegionListItem> list() {
		return regionRepository.findAllByOrderByRegionCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<RegionDetail> get(Long id) {
		return regionRepository.findById(id).map(this::toDetail);
	}

	@Transactional(readOnly = true)
	public CoverageOptions getOptions() {
		return buildCoverageOptions();
	}

	@Transactional
	public RegionDetail create(RegionRequest request) {
		RegionEntity entity = new RegionEntity();
		apply(entity, request);
		return toDetail(regionRepository.save(entity));
	}

	@Transactional
	public Optional<RegionDetail> update(Long id, RegionRequest request) {
		return regionRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(regionRepository.save(entity));
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
		validateBranchesExist(request.getBranches());
		entity.setRegionCode(request.getRegionCode());
		entity.setRegionName(request.getRegionName());
		entity.setStates(new LinkedHashSet<>(request.getStates()));
		entity.setZipCodes(new LinkedHashSet<>(request.getZipCodes()));
		entity.setBranches(new LinkedHashSet<>(request.getBranches()));
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private RegionListItem toListItem(RegionEntity entity) {
		Map<Long, BranchEntity> branchesById = branchRepository.findAllById(entity.getBranches()).stream()
				.collect(Collectors.toMap(BranchEntity::getId, Function.identity()));
		return new RegionListItem(
				entity.getId(),
				formatCodeAndName(entity.getRegionCode(), entity.getRegionName()),
				List.copyOf(entity.getStates()),
				List.copyOf(entity.getZipCodes()),
				entity.getBranches().stream()
						.map(branchesById::get)
						.filter(branch -> branch != null)
						.map(BranchEntity::getBranchCode)
						.toList(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private RegionDetail toDetail(RegionEntity entity) {
		Map<Long, BranchEntity> branchesById = branchRepository.findAllById(entity.getBranches()).stream()
				.collect(Collectors.toMap(BranchEntity::getId, Function.identity()));
		return new RegionDetail(
				entity.getId(),
				entity.getRegionCode(),
				entity.getRegionName(),
				List.copyOf(entity.getStates()),
				List.copyOf(entity.getZipCodes()),
				entity.getBranches().stream()
						.map(branchesById::get)
						.filter(branch -> branch != null)
						.map(branch -> new BranchOption(branch.getId(), branch.getBranchCode(), branch.getBranchName()))
						.toList(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private void validateBranchesExist(List<Long> branchIds) {
		Set<Long> uniqueBranchIds = new LinkedHashSet<>(branchIds);
		List<Long> existingBranchIds = branchRepository.findAllById(uniqueBranchIds).stream().map(BranchEntity::getId).toList();
		if (existingBranchIds.size() != uniqueBranchIds.size()) {
			throw new IllegalArgumentException("One or more branch IDs do not exist");
		}
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}

	private CoverageOptions buildCoverageOptions() {
		List<RegionEntity> regions = regionRepository.findAllByOrderByRegionCodeAsc();
		return new CoverageOptions(
				regions.stream().flatMap(region -> region.getStates().stream()).distinct().sorted().toList(),
				regions.stream().flatMap(region -> region.getZipCodes().stream()).distinct().sorted().toList(),
				branchRepository.findAllByOrderByBranchCodeAsc().stream()
						.map(branch -> new BranchOption(branch.getId(), branch.getBranchCode(), branch.getBranchName()))
						.toList()
		);
	}
}
