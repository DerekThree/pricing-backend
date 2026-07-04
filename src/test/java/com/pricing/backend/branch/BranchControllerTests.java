package com.pricing.backend.branch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.model.Branch;
import com.pricing.backend.generated.model.BranchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BranchControllerTests {

	private static final long MISSING_BRANCH_ID = 999L;

	private BranchService branchService;
	private BranchController branchController;

	@BeforeEach
	void setUp() {
		branchService = mock(BranchService.class);
		branchController = new BranchController(branchService);
	}

	@Test
	void createsBranch() {
		Branch createdBranch = getBranch();
		BranchRequest request = getRequest(createdBranch);
		when(branchService.create(request)).thenReturn(createdBranch);

		ResponseEntity<Branch> response = branchController.createBranch(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isEqualTo(createdBranch);
		verify(branchService).create(request);
	}

	@Test
	void listsBranches() {
		Branch firstBranch = getBranch();
		Branch secondBranch = new Branch(
				2L,
				"10000100",
				"Austin Central",
				"TX",
				"73301",
				OffsetDateTime.parse("2026-06-08T09:00:00+08:00"),
				"John Smith"
		);
		List<Branch> branches = List.of(firstBranch, secondBranch);
		when(branchService.list()).thenReturn(branches);

		ResponseEntity<List<Branch>> response = branchController.listBranches();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(branches);
		verify(branchService).list();
	}

	@Test
	void returnsStoredBranch() {
		Branch storedBranch = getBranch();
		when(branchService.get(storedBranch.getId())).thenReturn(Optional.of(storedBranch));

		ResponseEntity<Branch> response = branchController.getBranch(storedBranch.getId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(storedBranch);
		verify(branchService).get(storedBranch.getId());
	}

	@Test
	void updatesStoredBranch() {
		Branch updatedBranch = getBranch();
		BranchRequest request = getRequest(updatedBranch);
		when(branchService.update(updatedBranch.getId(), request)).thenReturn(Optional.of(updatedBranch));

		ResponseEntity<Branch> response = branchController.updateBranch(updatedBranch.getId(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(updatedBranch);
		verify(branchService).update(updatedBranch.getId(), request);
	}

	@Test
	void deletesStoredBranch() {
		Branch branch = getBranch();
		when(branchService.delete(branch.getId())).thenReturn(true);

		ResponseEntity<Void> response = branchController.deleteBranch(branch.getId());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(branchService).delete(branch.getId());
	}

	@Test
	void throwsWhenGettingMissingBranch() {
		when(branchService.get(MISSING_BRANCH_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> branchController.getBranch(MISSING_BRANCH_ID))
				.isInstanceOf(RecordNotFoundException.class);

		verify(branchService).get(MISSING_BRANCH_ID);
	}

	@Test
	void throwsWhenUpdatingMissingBranch() {
		BranchRequest request = getRequest(getBranch());
		when(branchService.update(MISSING_BRANCH_ID, request)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> branchController.updateBranch(MISSING_BRANCH_ID, request))
				.isInstanceOf(RecordNotFoundException.class);

		verify(branchService).update(MISSING_BRANCH_ID, request);
	}

	@Test
	void throwsWhenDeletingMissingBranch() {
		when(branchService.delete(MISSING_BRANCH_ID)).thenReturn(false);

		assertThatThrownBy(() -> branchController.deleteBranch(MISSING_BRANCH_ID))
				.isInstanceOf(RecordNotFoundException.class);

		verify(branchService).delete(MISSING_BRANCH_ID);
	}

	private Branch getBranch() {
		return new Branch(
				1L,
				"10000099",
				"Chicago Loop",
				"IL",
				"60601",
				OffsetDateTime.parse("2026-06-07T09:00:00+08:00"),
				"Jane Smith"
		);
	}

	private BranchRequest getRequest(Branch branch) {
		return new BranchRequest(
				branch.getBranchCode(),
				branch.getBranchName(),
				branch.getState(),
				branch.getZipCode(),
				branch.getUpdatedBy()
		);
	}
}
