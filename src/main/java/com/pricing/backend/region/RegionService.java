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
import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.model.BranchOption;
import com.pricing.backend.generated.model.RegionDetail;
import com.pricing.backend.generated.model.RegionListItem;
import com.pricing.backend.generated.model.RegionOptions;
import com.pricing.backend.generated.model.RegionRequest;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionService {

	private final RegionRepository regionRepository;
	private final BranchRepository branchRepository;
	private final PricingPlanRepository pricingPlanRepository;

	public RegionService(RegionRepository regionRepository, BranchRepository branchRepository,
			PricingPlanRepository pricingPlanRepository) {
		this.regionRepository = regionRepository;
		this.branchRepository = branchRepository;
		this.pricingPlanRepository = pricingPlanRepository;
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
	public RegionOptions getOptions(Long recordId) {
		RegionEntity record = recordId == null ? null : regionRepository.findById(recordId)
				.orElseThrow(() -> new RecordNotFoundException("Region", recordId));
		return buildRegionOptions(record);
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

		pricingPlanRepository.findFirstByRegionIdOrderByPlanCodeAsc(id)
				.ifPresent(pricingPlan -> {
					throw new RecordInUseException("region", "pricing plan", pricingPlan.getPlanCode());
				});

		regionRepository.deleteById(id);
		return true;
	}

	private void apply(RegionEntity entity, RegionRequest request) {
		validateBranchesExist(request.getBranches());
		entity.setRegionCode(request.getRegionCode());
		entity.setRegionName(request.getRegionName());
		entity.setStates(request.getStates());
		entity.setZipCodes(request.getZipCodes());
		entity.setBranches(request.getBranches());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

	private RegionListItem toListItem(RegionEntity entity) {
		Map<Long, BranchEntity> branchesById = branchRepository.findAllById(entity.getBranches()).stream()
				.collect(Collectors.toMap(BranchEntity::getId, Function.identity()));
		return new RegionListItem(
				entity.getId(),
				formatCodeAndName(entity.getRegionCode(), entity.getRegionName()),
				entity.getStates(),
				entity.getZipCodes(),
				entity.getBranches().stream()
						.map(branchesById::get)
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
				entity.getRegionCode(),
				entity.getRegionName(),
				entity.getStates(),
				entity.getZipCodes(),
				entity.getBranches(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn(),
				new RegionOptions(
						entity.getStates(),
						entity.getZipCodes(),
						entity.getBranches().stream()
								.map(branchesById::get)
								.map(branch -> new BranchOption(branch.getId(), branch.getBranchCode(), branch.getBranchName()))
								.toList()
				)
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

	private RegionOptions buildRegionOptions(RegionEntity record) {
		List<BranchEntity> branches = branchRepository.findAllByOrderByBranchCodeAsc();
		List<RegionEntity> regions = regionRepository.findAllByOrderByRegionCodeAsc().stream()
				.filter(region -> record == null || !region.getId().equals(record.getId()))
				.toList();

		Set<String> usedStates = regions.stream().flatMap(region -> region.getStates().stream()).collect(Collectors.toSet());
		Set<String> usedZipCodes = regions.stream().flatMap(region -> region.getZipCodes().stream()).collect(Collectors.toSet());
		Set<Long> usedBranchIds = regions.stream().flatMap(region -> region.getBranches().stream()).collect(Collectors.toSet());
		
		List<String> availableStates = branches.stream().map(BranchEntity::getState)
				.filter(state -> !usedStates.contains(state))
				.distinct()
				.sorted()
				.toList();
		List<String> availableZipCodes = branches.stream().map(BranchEntity::getZipCode)
				.filter(zipCode -> !usedZipCodes.contains(zipCode))
				.distinct()
				.sorted()
				.toList();
		List<BranchOption> availableBranches = branches.stream()
				.filter(branch -> !usedBranchIds.contains(branch.getId()))
				.map(this::toBranchOption)
				.toList();
		
				return new RegionOptions(availableStates, availableZipCodes, availableBranches);
	}

	private BranchOption toBranchOption(BranchEntity branch) {
		return new BranchOption(branch.getId(), branch.getBranchCode(), branch.getBranchName());
	}

}
