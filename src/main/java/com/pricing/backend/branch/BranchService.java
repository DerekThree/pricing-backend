package com.pricing.backend.branch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordInUseException;
import com.pricing.backend.generated.model.BranchDetail;
import com.pricing.backend.generated.model.BranchListItem;
import com.pricing.backend.generated.model.BranchRequest;
import com.pricing.backend.region.RegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

	private static final Logger log = LoggerFactory.getLogger(BranchService.class);

	private final BranchRepository branchRepository;
	private final RegionRepository regionRepository;
	private final BranchMapper mapper;

	public BranchService(BranchRepository branchRepository, RegionRepository regionRepository, BranchMapper mapper) {
		this.branchRepository = branchRepository;
		this.regionRepository = regionRepository;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<BranchListItem> list() {
		return branchRepository.findAllByOrderByBranchCodeAsc().stream()
				.map(mapper::toBranchListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<BranchDetail> get(Long id) {
		return branchRepository.findById(id).map(mapper::toBranchDetail);
	}

	@Transactional
	public BranchDetail create(BranchRequest request) {
		log.info("Create Branch requested: code={}", request.getBranchCode());
		BranchEntity entity = new BranchEntity();
		apply(entity, request);
		return mapper.toBranchDetail(branchRepository.save(entity));
	}

	@Transactional
	public Optional<BranchDetail> update(Long id, BranchRequest request) {
		log.info("Update Branch requested: id={}", id);
		return branchRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return mapper.toBranchDetail(branchRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		log.info("Delete Branch requested: id={}", id);
		if (!branchRepository.existsById(id)) {
			return false;
		}

		regionRepository.findFirstByBranchesContainsOrderByRegionCodeAsc(id)
				.ifPresent(region -> {
					throw new RecordInUseException("branch", "region", region.getRegionCode());
				});

		branchRepository.deleteById(id);
		return true;
	}

	private void apply(BranchEntity entity, BranchRequest request) {
		entity.setBranchCode(request.getBranchCode());
		entity.setBranchName(request.getBranchName());
		entity.setState(request.getState());
		entity.setZipCode(request.getZipCode());
		entity.setUpdatedBy(request.getUpdatedBy());
		entity.setUpdatedOn(OffsetDateTime.now());
	}

}
