package com.pricing.backend.branch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.generated.model.Branch;
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
	public List<Branch> list() {
		return branchRepository.findAllByOrderByBranchCodeAsc().stream()
				.map(this::toModel)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<Branch> get(Long id) {
		return branchRepository.findById(id).map(this::toModel);
	}

	@Transactional
	public Branch create(BranchRequest request) {
		BranchEntity entity = new BranchEntity();
		apply(entity, request);
		return toModel(branchRepository.save(entity));
	}

	@Transactional
	public Optional<Branch> update(Long id, BranchRequest request) {
		return branchRepository.findById(id)
				.map(entity -> {
					apply(entity, request);
					return toModel(branchRepository.save(entity));
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

	private Branch toModel(BranchEntity entity) {
		return new Branch(
				entity.getId(),
				entity.getBranchCode(),
				entity.getBranchName(),
				entity.getState(),
				entity.getZipCode(),
				entity.getUpdatedOn(),
				entity.getUpdatedBy()
		);
	}
}
