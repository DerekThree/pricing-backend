package com.pricing.backend.branch;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BranchRepositoryTests {

	@Autowired
	private BranchRepository branchRepository;

	@Test
	void createsBranch() {
		BranchEntity branch = new BranchEntity(
				null,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		);

		BranchEntity savedBranch = branchRepository.save(branch);
		try {
			assertThat(savedBranch.getId()).isNotNull();
			assertThat(savedBranch.getBranchCode()).isEqualTo(branch.getBranchCode());
			assertThat(savedBranch.getBranchName()).isEqualTo(branch.getBranchName());
			assertThat(savedBranch.getState()).isEqualTo(branch.getState());
			assertThat(savedBranch.getZipCode()).isEqualTo(branch.getZipCode());
			assertThat(savedBranch.getUpdatedOn()).isEqualTo(branch.getUpdatedOn());
			assertThat(savedBranch.getUpdatedBy()).isEqualTo(branch.getUpdatedBy());
		} finally {
			branchRepository.deleteById(savedBranch.getId());
		}
	}

	@Test
	void readsStoredBranch() {
		BranchEntity savedBranch = branchRepository.save(new BranchEntity(
				null,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
		try {
			BranchEntity retrievedBranch = branchRepository.findById(savedBranch.getId()).orElseThrow();

			assertThat(retrievedBranch).usingRecursiveComparison().isEqualTo(savedBranch);
		} finally {
			branchRepository.deleteById(savedBranch.getId());
		}
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
		try {
			savedBranch.setBranchCode("10000099");
			savedBranch.setBranchName("Chicago Loop");
			savedBranch.setZipCode("60601");
			savedBranch.setUpdatedBy("Jane Smith");
			savedBranch.setUpdatedOn(OffsetDateTime.parse("2026-06-07T09:00:00+08:00"));

			BranchEntity updatedBranch = branchRepository.save(savedBranch);

			assertThat(updatedBranch.getId()).isEqualTo(savedBranch.getId());
			assertThat(updatedBranch.getBranchCode()).isEqualTo("10000099");
			assertThat(updatedBranch.getBranchName()).isEqualTo("Chicago Loop");
			assertThat(updatedBranch.getZipCode()).isEqualTo("60601");
			assertThat(updatedBranch.getUpdatedBy()).isEqualTo("Jane Smith");
			assertThat(updatedBranch.getUpdatedOn())
					.isEqualTo(OffsetDateTime.parse("2026-06-07T09:00:00+08:00"));
		} finally {
			branchRepository.deleteById(savedBranch.getId());
		}
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
		try {
			branchRepository.deleteById(savedBranch.getId());

			assertThat(branchRepository.findById(savedBranch.getId())).isEmpty();
		} finally {
			branchRepository.findById(savedBranch.getId()).ifPresent(branchRepository::delete);
		}
	}
}
