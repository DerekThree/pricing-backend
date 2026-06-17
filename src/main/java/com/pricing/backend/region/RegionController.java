package com.pricing.backend.region;

import java.util.List;

import com.pricing.backend.generated.api.RegionsApi;
import com.pricing.backend.generated.model.Region;
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
	public ResponseEntity<Region> getRegion(Long id) {
		return regionService.get(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Region> createRegion(RegionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(regionService.create(request));
	}

	@Override
	public ResponseEntity<Region> updateRegion(Long id, RegionRequest request) {
		return regionService.update(id, request)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Void> deleteRegion(Long id) {
		if (regionService.delete(id)) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.notFound().build();
	}
}
