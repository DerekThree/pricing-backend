package com.pricing.backend.batch;

import com.pricing.backend.generated.model.AccountAttribute;
import com.pricing.backend.generated.model.BatchAccount;
import com.pricing.backend.generated.model.BatchAccountResult;
import com.pricing.backend.generated.model.BatchAccountStatus;
import com.pricing.backend.generated.model.BatchFeeRequest;
import com.pricing.backend.generated.model.BatchFeeResult;
import com.pricing.backend.generated.model.BatchFeeStatus;
import com.pricing.backend.generated.model.BatchRequest;
import com.pricing.backend.eligibilityreason.AccountAttributeScalarValue;
import com.pricing.backend.generated.model.Decision;
import com.pricing.engine.RuleEngineBatch;
import com.pricing.engine.RuleEngineBatch.*;
import com.pricing.engine.RuleEngineBatchResult;
import com.pricing.engine.RuleEngineBatchResult.*;
import org.springframework.stereotype.Component;

@Component
class BatchMapper {

	RuleEngineBatch toRuleEngineBatch(BatchRequest request) {
		return new RuleEngineBatch(request.getBatchId(), request.getAccounts().stream()
				.map(this::toAccount)
				.toList());
	}

	com.pricing.backend.generated.model.BatchResult toResponse(RuleEngineBatchResult result) {
		return new com.pricing.backend.generated.model.BatchResult(
				result.batchId(),
				result.accounts().stream().map(this::toAccountResult).toList());
	}

	private Account toAccount(BatchAccount account) {
		return new Account(
				account.getAccountNumber(),
				account.getProductCode(),
				account.getBranchCode(),
				account.getPricingDate(),
				account.getAttributes().stream().map(this::toAccountAttribute).toList(),
				account.getFees().stream().map(this::toFeeRequest).toList());
	}

	private RuleEngineBatch.AccountAttribute toAccountAttribute(AccountAttribute attribute) {
		AccountAttributeScalarValue value = (AccountAttributeScalarValue) attribute.getValue();
		return new RuleEngineBatch.AccountAttribute(attribute.getCode(), value.getValue());
	}

	private FeeRequest toFeeRequest(BatchFeeRequest fee) {
		return new FeeRequest(fee.getFeeRequestId(), fee.getCode(), fee.getTransactionAmount());
	}

	private BatchAccountResult toAccountResult(AccountResult result) {
		return new BatchAccountResult(
				result.accountNumber(),
				BatchAccountStatus.valueOf(result.status().name()))
				.pricingPlanCode(result.pricingPlanCode())
				.fees(result.fees() == null ? null : result.fees().stream()
						.map(this::toFeeResult)
						.toList());
	}

	private BatchFeeResult toFeeResult(FeeResult result) {
		return new BatchFeeResult(
				result.feeRequestId(),
				BatchFeeStatus.valueOf(result.status().name()))
				.decision(result.decision() == null ? null : Decision.valueOf(result.decision().name()))
				.amount(result.amount())
				.reasons(result.reasons());
	}
}
