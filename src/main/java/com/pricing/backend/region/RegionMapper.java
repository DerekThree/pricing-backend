package com.pricing.backend.region;

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

	RegionDetail toRegionDetail(RegionEntity entity) {
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
