package com.pricing.backend.region;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.generated.model.BranchOption;
import com.pricing.backend.generated.model.RegionDetail;
import com.pricing.backend.generated.model.RegionListItem;
import com.pricing.backend.generated.model.RegionOptions;
import org.springframework.stereotype.Component;

@Component
class RegionMapper {

	private final BranchRepository branchRepository;

	RegionMapper(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	RegionListItem toRegionListItem(RegionEntity entity) {
		List<String> states = List.copyOf(entity.getStates());
		List<String> zipCodes = List.copyOf(entity.getZipCodes());
		List<Long> branchIds = List.copyOf(entity.getBranches());
		Map<Long, BranchEntity> branchesById = branchRepository.findAllById(branchIds).stream()
				.collect(Collectors.toMap(BranchEntity::getId, Function.identity()));
		return new RegionListItem(
				entity.getId(),
				formatCodeAndName(entity.getRegionCode(), entity.getRegionName()),
				states,
				zipCodes,
				branchIds.stream()
						.map(branchesById::get)
						.map(BranchEntity::getBranchCode)
						.toList(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	RegionDetail toRegionDetail(RegionEntity entity) {
		List<String> states = List.copyOf(entity.getStates());
		List<String> zipCodes = List.copyOf(entity.getZipCodes());
		List<Long> branchIds = List.copyOf(entity.getBranches());
		Map<Long, BranchEntity> branchesById = branchRepository.findAllById(branchIds).stream()
				.collect(Collectors.toMap(BranchEntity::getId, Function.identity()));
		return new RegionDetail(
				entity.getRegionCode(),
				entity.getRegionName(),
				states,
				zipCodes,
				branchIds,
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn(),
				new RegionOptions(
						states,
						zipCodes,
						branchIds.stream()
								.map(branchesById::get)
								.map(this::toBranchOption)
								.toList()
				)
		);
	}

	BranchOption toBranchOption(BranchEntity branch) {
		return new BranchOption(branch.getId(), branch.getBranchCode(), branch.getBranchName());
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
