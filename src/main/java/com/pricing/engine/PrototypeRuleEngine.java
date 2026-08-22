package com.pricing.engine;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.pricing.engine.AccountBatch.Account;
import com.pricing.engine.AccountBatch.FeeRequest;
import com.pricing.engine.AccountBatchResult.AccountResult;
import com.pricing.engine.AccountBatchResult.AccountStatus;
import com.pricing.engine.AccountBatchResult.Decision;
import com.pricing.engine.AccountBatchResult.FeeResult;
import com.pricing.engine.AccountBatchResult.FeeStatus;
import com.pricing.engine.PriceConfig.Branch;
import com.pricing.engine.PriceConfig.FeeType;
import com.pricing.engine.PriceConfig.Plan;
import com.pricing.engine.PriceConfig.PlanFee;
import com.pricing.engine.PriceConfig.Region;
import org.springframework.stereotype.Component;

@Component
public class PrototypeRuleEngine implements RuleEngine {

	private final PriceConfigRepository configRepository;

	public PrototypeRuleEngine(PriceConfigRepository configRepository) {
		this.configRepository = configRepository;
	}

	@Override
	public AccountBatchResult price(AccountBatch batch) {
		Set<LocalDate> pricingDates = batch.accounts().stream()
				.map(Account::pricingDate)
				.collect(Collectors.toUnmodifiableSet());
		PriceConfig config = configRepository.load(pricingDates);
		return new AccountBatchResult(batch.batchId(), batch.accounts().stream()
				.map(account -> price(account, config))
				.toList());
	}

	private AccountResult price(Account account, PriceConfig config) {
		if (!config.productCodes().contains(account.productCode())) {
			return failed(account, AccountStatus.PRODUCT_NOT_FOUND);
		}

		Branch branch = config.branches().get(account.branchCode());
		if (branch == null) {
			return failed(account, AccountStatus.BRANCH_NOT_FOUND);
		}

		List<Region> regionMatches = config.regions().stream()
				.filter(candidate -> candidate.branchCodes().contains(branch.code()))
				.toList();
		if (regionMatches.isEmpty()) {
			regionMatches = config.regions().stream()
					.filter(candidate -> candidate.zipCodes().contains(branch.zipCode()))
					.toList();
		}
		if (regionMatches.isEmpty()) {
			regionMatches = config.regions().stream()
					.filter(candidate -> candidate.states().contains(branch.state()))
					.toList();
		}
		if (regionMatches.isEmpty()) {
			return failed(account, AccountStatus.REGION_NOT_FOUND);
		}
		if (regionMatches.size() > 1) {
			return failed(account, AccountStatus.ERROR);
		}
		Region region = regionMatches.getFirst();
		List<Plan> planMatches = config.plans().stream()
				.filter(candidate -> candidate.productCode().equals(account.productCode()))
				.filter(candidate -> candidate.regionCode().equals(region.code()))
				.filter(candidate -> !account.pricingDate().isBefore(candidate.activeFrom()))
				.filter(candidate -> !account.pricingDate().isAfter(candidate.activeThrough()))
				.toList();
		if (planMatches.isEmpty()) {
			return failed(account, AccountStatus.PLAN_NOT_FOUND);
		}
		if (planMatches.size() > 1) {
			return failed(account, AccountStatus.ERROR);
		}
		Plan plan = planMatches.getFirst();
		List<FeeResult> fees = account.fees().stream()
				.map(request -> price(request, plan))
				.toList();
		return new AccountResult(account.accountNumber(), AccountStatus.OK, plan.code(), fees);
	}

	private AccountResult failed(Account account, AccountStatus status) {
		return new AccountResult(account.accountNumber(), status, null, null);
	}

	private FeeResult price(FeeRequest request, Plan plan) {
		PlanFee fee = requireOne(plan.fees().stream()
				.filter(candidate -> candidate.code().equals(request.code()))
				.toList(), "Pricing Plan Fee");
		if (fee.type() != FeeType.FLAT) {
			throw new IllegalStateException("Fee is outside the flat-Fee tracer");
		}

		return new FeeResult(
				request.feeRequestId(),
				FeeStatus.OK,
				Decision.CHARGED,
				fee.amount().setScale(2, RoundingMode.HALF_UP),
				null);
	}

	private <T> T requireOne(List<T> matches, String configurationType) {
		if (matches.size() != 1) {
			throw new IllegalStateException(configurationType + " is outside the flat-Fee tracer");
		}

		return matches.getFirst();
	}
}
