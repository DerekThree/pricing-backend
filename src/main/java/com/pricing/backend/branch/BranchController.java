package com.pricing.backend.branch;

import java.util.List;

import com.pricing.backend.generated.api.BranchesApi;
import com.pricing.backend.generated.model.Branch;
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
	public ResponseEntity<List<Branch>> listBranches() {
		return ResponseEntity.ok(branchService.list());
	}

	@Override
	public ResponseEntity<Branch> getBranch(Long id) {
		return branchService.get(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Branch> createBranch(BranchRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(request));
	}

	@Override
	public ResponseEntity<Branch> updateBranch(Long id, BranchRequest request) {
		return branchService.update(id, request)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Void> deleteBranch(Long id) {
		if (branchService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.notFound().build();
	}
}
