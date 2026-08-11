package com.pricing.backend.simulator;

import java.time.LocalDate;

import com.pricing.backend.generated.api.SimulatorApi;
import com.pricing.backend.generated.model.SimulatorDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulatorController implements SimulatorApi {

	private final SimulatorDateService simulatorDateService;

	public SimulatorController(SimulatorDateService simulatorDateService) {
		this.simulatorDateService = simulatorDateService;
	}

	@Override
	public ResponseEntity<SimulatorDate> getSimulatorDate() {
		return ResponseEntity.ok(new SimulatorDate(simulatorDateService.getCurrentDate()));
	}

	@Override
	public ResponseEntity<SimulatorDate> setSimulatorDate(SimulatorDate request) {
		LocalDate currentDate = simulatorDateService.setCurrentDate(request.getCurrentDate());
		return ResponseEntity.ok(new SimulatorDate(currentDate));
	}
}
