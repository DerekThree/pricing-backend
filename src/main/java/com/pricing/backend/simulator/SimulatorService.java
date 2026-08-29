package com.pricing.backend.simulator;

import java.time.Clock;
import java.time.LocalDate;

import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.SimulatorOptions;
import com.pricing.backend.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulatorService {

	private final Clock systemClock;
	private final ProductRepository productRepository;
	private final BranchRepository branchRepository;
	private final FeeRepository feeRepository;
	private final AccountAttributeRepository accountAttributeRepository;
	private final SimulatorMapper simulatorMapper;
	private volatile Clock applicationClock;

	public SimulatorService(Clock systemClock, ProductRepository productRepository,
			BranchRepository branchRepository, FeeRepository feeRepository,
			AccountAttributeRepository accountAttributeRepository, SimulatorMapper simulatorMapper) {
		this.systemClock = systemClock;
		this.productRepository = productRepository;
		this.branchRepository = branchRepository;
		this.feeRepository = feeRepository;
		this.accountAttributeRepository = accountAttributeRepository;
		this.simulatorMapper = simulatorMapper;
		this.applicationClock = systemClock;
	}

	public LocalDate getCurrentDate() {
		return LocalDate.now(applicationClock);
	}

	public LocalDate setCurrentDate(LocalDate currentDate) {
		applicationClock = Clock.fixed(currentDate.atStartOfDay(systemClock.getZone()).toInstant(), systemClock.getZone());
		return getCurrentDate();
	}

	@Transactional(readOnly = true)
	public SimulatorOptions getOptions() {
		return new SimulatorOptions(
				productRepository.findAllByOrderByProductCodeAsc().stream()
						.map(simulatorMapper::toProductOption)
						.toList(),
				branchRepository.findAllByOrderByBranchCodeAsc().stream()
						.map(simulatorMapper::toBranchOption)
						.toList(),
				feeRepository.findAllByOrderByFeeCodeAsc().stream()
						.map(simulatorMapper::toFeeOption)
						.toList(),
				accountAttributeRepository.findAllByOrderByAttributeCodeAsc().stream()
						.map(simulatorMapper::toAttributeOption)
						.toList()
		);
	}
}
