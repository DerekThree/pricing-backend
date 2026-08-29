package com.pricing.backend.simulator;

import java.time.LocalDate;

import com.pricing.backend.generated.api.SimulatorApi;
import com.pricing.backend.generated.model.SimulatorDate;
import com.pricing.backend.generated.model.SimulatorOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulatorController implements SimulatorApi {

	private final SimulatorService simulatorService;

	public SimulatorController(SimulatorService simulatorService) {
		this.simulatorService = simulatorService;
	}

	@Override
	public ResponseEntity<SimulatorDate> getSimulatorDate() {
		return ResponseEntity.ok(new SimulatorDate(simulatorService.getCurrentDate()));
	}

	@Override
	public ResponseEntity<SimulatorOptions> getSimulatorOptions() {
		return ResponseEntity.ok(simulatorService.getOptions());
	}

	@Override
	public ResponseEntity<SimulatorDate> setSimulatorDate(SimulatorDate request) {
		LocalDate currentDate = simulatorService.setCurrentDate(request.getCurrentDate());
		return ResponseEntity.ok(new SimulatorDate(currentDate));
	}

}
