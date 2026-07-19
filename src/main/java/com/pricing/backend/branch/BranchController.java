package com.pricing.backend.branch;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.BranchesApi;
import com.pricing.backend.generated.model.BranchDetail;
import com.pricing.backend.generated.model.BranchListItem;
import com.pricing.backend.generated.model.BranchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BranchController implements BranchesApi {

	private final BranchService branchService;

	public BranchController(BranchService branchService) {
		this.branchService = branchService;
	}

	@Override
	public ResponseEntity<List<BranchListItem>> listBranches() {
		return ResponseEntity.ok(branchService.list());
	}

	@Override
	public ResponseEntity<BranchDetail> getBranch(Long id) {
		BranchDetail branch = branchService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Branch", id));

		return ResponseEntity.ok(branch);
	}

	@Override
	public ResponseEntity<BranchDetail> createBranch(BranchRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(request));
	}

	@Override
	public ResponseEntity<BranchDetail> updateBranch(Long id, BranchRequest request) {
		BranchDetail branch = branchService.update(id, request)
				.orElseThrow(() -> new RecordNotFoundException("Branch", id));

		return ResponseEntity.ok(branch);
	}

	@Override
	public ResponseEntity<Void> deleteBranch(Long id) {
		if (branchService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Branch", id);
	}
}
