package com.pricing.backend.region;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionService {

	private static final Logger log = LoggerFactory.getLogger(RegionService.class);

	private final RegionRepository regionRepository;
	private final BranchRepository branchRepository;
	private final PricingPlanRepository pricingPlanRepository;
	private final RegionMapper mapper;

	public RegionService(RegionRepository regionRepository, BranchRepository branchRepository,
			PricingPlanRepository pricingPlanRepository, RegionMapper mapper) {
		this.regionRepository = regionRepository;
		this.branchRepository = branchRepository;
		this.pricingPlanRepository = pricingPlanRepository;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<RegionListItem> list() {
		return regionRepository.findAllByOrderByRegionCodeAsc().stream()
				.map(mapper::toRegionListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<RegionDetail> get(Long id) {
		return regionRepository.findById(id).map(mapper::toRegionDetail);
	}

	@Transactional(readOnly = true)
	public RegionOptions getOptions(Long recordId) {
		RegionEntity record = recordId == null ? null : regionRepository.findById(recordId)
				.orElseThrow(() -> new RecordNotFoundException("Region", recordId));
		return buildRegionOptions(record);
	}

	@Transactional
	public RegionDetail create(RegionRequest request) {
		log.info("Create Region requested: code={}", request.getRegionCode());
		RegionEntity entity = new RegionEntity();
		apply(entity, request);
		return mapper.toRegionDetail(regionRepository.save(entity));
	}

	@Transactional
	public Optional<RegionDetail> update(Long id, RegionRequest request) {
		log.info("Update Region requested: id={}", id);
		return regionRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return mapper.toRegionDetail(regionRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		log.info("Delete Region requested: id={}", id);
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
		entity.setRegionCode(request.getRegionCode());
		entity.setRegionName(request.getRegionName());
		entity.setStates(request.getStates());
		entity.setZipCodes(request.getZipCodes());
		entity.setBranches(request.getBranches());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
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
				.map(mapper::toBranchOption)
				.toList();

		return new RegionOptions(availableStates, availableZipCodes, availableBranches);
	}

}
