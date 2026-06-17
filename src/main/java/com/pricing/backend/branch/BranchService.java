package com.pricing.backend.branch;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.pricing.backend.generated.model.Branch;
import com.pricing.backend.generated.model.BranchRequest;
import org.springframework.stereotype.Service;

@Service
public class BranchService {

	private final Map<Long, Branch> branches = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(3);

	public BranchService() {
		branches.put(1L, new Branch(
				1L,
				"10000001",
				"Chicago 104th",
				"IL",
				"60459",
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
		branches.put(2L, new Branch(
				2L,
				"10000002",
				"Austin Mopec",
				"TX",
				"78759",
				OffsetDateTime.parse("2026-06-06T09:15:00+08:00"),
				"John Smith"
		));
	}

	public List<Branch> list() {
		return branches.values().stream()
				.sorted(Comparator.comparing(Branch::getBranchCode))
				.toList();
	}

	public Optional<Branch> get(Long id) {
		return Optional.ofNullable(branches.get(id));
	}

	public Branch create(BranchRequest request) {
		Branch branch = new Branch(
				nextId.getAndIncrement(),
				request.getBranchCode(),
				request.getBranchName(),
				request.getState(),
				request.getZipCode(),
				now(),
				request.getUpdatedBy()
		);
		branches.put(branch.getId(), branch);
		return branch;
	}

	public Optional<Branch> update(Long id, BranchRequest request) {
		Optional<Branch> existingBranch = get(id);
		if (existingBranch.isEmpty()) {
			return Optional.empty();
		}

		Branch branch = new Branch(
				id,
				request.getBranchCode(),
				request.getBranchName(),
				request.getState(),
				request.getZipCode(),
				now(),
				request.getUpdatedBy()
		);
		branches.put(id, branch);
		return Optional.of(branch);
	}

	public boolean delete(Long id) {
		return branches.remove(id) != null;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now();
	}
}
