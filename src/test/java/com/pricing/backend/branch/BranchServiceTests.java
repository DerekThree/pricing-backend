package com.pricing.backend.branch;

import java.time.OffsetDateTime;

import com.pricing.backend.generated.model.Branch;
import com.pricing.backend.generated.model.BranchRequest;
import com.pricing.backend.region.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BranchServiceTests {

	@Autowired
	private BranchService branchService;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private RegionRepository regionRepository;

	@BeforeEach
	void setUp() {
		regionRepository.deleteAll();
		branchRepository.deleteAll();
	}

	@Test
	void createsBranch() {
		BranchRequest request = new BranchRequest(
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				"Derek Ochal"
		);

		Branch createdBranch = branchService.create(request);

		assertThat(createdBranch.getId()).isNotNull();
		assertThat(createdBranch.getBranchCode()).isEqualTo(request.getBranchCode());
		assertThat(createdBranch.getBranchName()).isEqualTo(request.getBranchName());
		assertThat(createdBranch.getState()).isEqualTo(request.getState());
		assertThat(createdBranch.getZipCode()).isEqualTo(request.getZipCode());
		assertThat(createdBranch.getUpdatedBy()).isEqualTo(request.getUpdatedBy());
		assertThat(createdBranch.getUpdatedOn()).isNotNull();
		assertThat(branchRepository.findById(createdBranch.getId())).isPresent();
	}

	@Test
	void returnsStoredBranch() {
		BranchEntity savedBranch = branchRepository.save(new BranchEntity(
				null,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));

		Branch returnedBranch = branchService.get(savedBranch.getId()).orElseThrow();

		assertThat(returnedBranch).isEqualTo(new Branch(
				savedBranch.getId(),
				savedBranch.getBranchCode(),
				savedBranch.getBranchName(),
				savedBranch.getState(),
				savedBranch.getZipCode(),
				savedBranch.getUpdatedOn(),
				savedBranch.getUpdatedBy()
		));
	}

	@Test
	void updatesStoredBranch() {
		BranchEntity savedBranch = branchRepository.save(new BranchEntity(
				null,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
		BranchRequest request = new BranchRequest(
				"10000099",
				"Chicago Loop",
				"IL",
				"60601",
				"Jane Smith"
		);

		Branch updatedBranch = branchService.update(savedBranch.getId(), request).orElseThrow();

		assertThat(updatedBranch.getId()).isEqualTo(savedBranch.getId());
		assertThat(updatedBranch.getBranchCode()).isEqualTo(request.getBranchCode());
		assertThat(updatedBranch.getBranchName()).isEqualTo(request.getBranchName());
		assertThat(updatedBranch.getState()).isEqualTo(request.getState());
		assertThat(updatedBranch.getZipCode()).isEqualTo(request.getZipCode());
		assertThat(updatedBranch.getUpdatedBy()).isEqualTo(request.getUpdatedBy());
		assertThat(updatedBranch.getUpdatedOn()).isNotNull();
		assertThat(updatedBranch.getUpdatedOn()).isAfter(savedBranch.getUpdatedOn());
	}

	@Test
	void deletesStoredBranch() {
		BranchEntity savedBranch = branchRepository.save(new BranchEntity(
				null,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));

		boolean deleted = branchService.delete(savedBranch.getId());

		assertThat(deleted).isTrue();
		assertThat(branchRepository.findById(savedBranch.getId())).isEmpty();
	}
}
