package com.pricing.backend.branch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.BranchDetail;
import com.pricing.backend.generated.model.BranchListItem;
import com.pricing.backend.generated.model.BranchRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

	private final BranchRepository branchRepository;

	public BranchService(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	@Transactional(readOnly = true)
	public List<BranchListItem> list() {
		return branchRepository.findAllByOrderByBranchCodeAsc().stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<BranchDetail> get(Long id) {
		return branchRepository.findById(id).map(this::toDetail);
	}

	@Transactional
	public BranchDetail create(BranchRequest request) {
		BranchEntity entity = new BranchEntity();
		apply(entity, request);
		return toDetail(branchRepository.save(entity));
	}

	@Transactional
	public Optional<BranchDetail> update(Long id, BranchRequest request) {
		return branchRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toDetail(branchRepository.save(entity));
				});
	}

	@Transactional
	public boolean delete(Long id) {
		if (!branchRepository.existsById(id)) {
			return false;
		}

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

	private BranchListItem toListItem(BranchEntity entity) {
		return new BranchListItem(
				entity.getId(),
				formatCodeAndName(entity.getBranchCode(), entity.getBranchName()),
				entity.getState(),
				entity.getZipCode(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}

	private BranchDetail toDetail(BranchEntity entity) {
		return new BranchDetail(
				entity.getBranchCode(),
				entity.getBranchName(),
				entity.getState(),
				entity.getZipCode(),
				entity.getUpdatedBy(),
				entity.getId(),
				entity.getUpdatedOn()
		);
	}

	private String formatCodeAndName(String code, String name) {
		return code + " - " + name;
	}
}
