package com.pricing.backend.region;

import java.util.List;

import com.pricing.backend.config.RecordNotFoundException;
import com.pricing.backend.generated.api.RegionsApi;
import com.pricing.backend.generated.model.Region;
import com.pricing.backend.generated.model.RegionOptions;
import com.pricing.backend.generated.model.RegionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegionController implements RegionsApi {

	private final RegionService regionService;

	public RegionController(RegionService regionService) {
		this.regionService = regionService;
	}

	@Override
	public ResponseEntity<List<Region>> listRegions() {
		return ResponseEntity.ok(regionService.list());
	}

	@Override
	public ResponseEntity<RegionOptions> getRegionOptions() {
		return ResponseEntity.ok(regionService.options());
	}

	@Override
	public ResponseEntity<Region> getRegion(Long id) {
		Region region = regionService.get(id)
				.orElseThrow(() -> new RecordNotFoundException("Region", id));

		return ResponseEntity.ok(region);
	}

	@Override
	public ResponseEntity<Region> createRegion(RegionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(regionService.create(request));
	}

	@Override
	public ResponseEntity<Region> updateRegion(Long id, RegionRequest request) {
		Region region = regionService.update(id, request)
				.orElseThrow(() -> new RecordNotFoundException("Region", id));

		return ResponseEntity.ok(region);
	}

	@Override
	public ResponseEntity<Void> deleteRegion(Long id) {
		if (regionService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		throw new RecordNotFoundException("Region", id);
	}
}
