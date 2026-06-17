package com.pricing.backend.region;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.pricing.backend.generated.model.Region;
import com.pricing.backend.generated.model.RegionRequest;
import org.springframework.stereotype.Service;

@Service
public class RegionService {

	private final Map<Long, Region> regions = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(3);

	public RegionService() {
		regions.put(1L, new Region(
				1L,
				"MIDWEST1",
				"Midwest",
				Set.of("IL", "IN", "MI", "OH", "WI"),
				Set.of("60459", "60601", "46204", "48201", "53202"),
				Set.of("10000001"),
				OffsetDateTime.parse("2026-06-06T09:30:00+08:00"),
				"Derek Ochal"
		));
		regions.put(2L, new Region(
				2L,
				"SOUTH001",
				"South",
				Set.of("TX", "FL", "GA"),
				Set.of("78759", "33101", "30301"),
				Set.of("10000002"),
				OffsetDateTime.parse("2026-06-06T09:45:00+08:00"),
				"John Smith"
		));
	}

	public List<Region> list() {
		return regions.values().stream()
				.sorted(Comparator.comparing(Region::getRegionCode))
				.toList();
	}

	public Optional<Region> get(Long id) {
		return Optional.ofNullable(regions.get(id));
	}

	public Region create(RegionRequest request) {
		Region region = new Region(
				nextId.getAndIncrement(),
				request.getRegionCode(),
				request.getRegionName(),
				request.getStates(),
				request.getZipCodes(),
				request.getBranches(),
				now(),
				request.getUpdatedBy()
		);
		regions.put(region.getId(), region);
		return region;
	}

	public Optional<Region> update(Long id, RegionRequest request) {
		Optional<Region> existingRegion = get(id);
		if (existingRegion.isEmpty()) {
			return Optional.empty();
		}

		Region region = new Region(
				id,
				request.getRegionCode(),
				request.getRegionName(),
				request.getStates(),
				request.getZipCodes(),
				request.getBranches(),
				now(),
				request.getUpdatedBy()
		);
		regions.put(id, region);
		return Optional.of(region);
	}

	public boolean delete(Long id) {
		return regions.remove(id) != null;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now();
	}
}
