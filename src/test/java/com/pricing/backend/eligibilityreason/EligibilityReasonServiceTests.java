package com.pricing.backend.eligibilityreason;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.generated.model.AccountAttributeType;
import com.pricing.backend.generated.model.EligibilityReasonCondition;
import com.pricing.backend.generated.model.EligibilityReasonDetail;
import com.pricing.backend.generated.model.EligibilityReasonOperator;
import com.pricing.backend.generated.model.EligibilityReasonRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EligibilityReasonServiceTests {

	@Autowired
	private EligibilityReasonService eligibilityReasonService;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

	@Autowired
	private AccountAttributeRepository accountAttributeRepository;

	@BeforeEach
	void setUp() {
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
	}

	@Test
	void createsEligibilityReason() {
		AccountAttributeEntity amount = saveAttribute("ATTR0001", "Min Amount", AccountAttributeType.DECIMAL);
		AccountAttributeEntity active = saveAttribute("ATTR0002", "Active", AccountAttributeType.BOOLEAN);
		EligibilityReasonRequest request = new EligibilityReasonRequest(
				"ELIG0001",
				"Min. Balance",
				List.of(
						new EligibilityReasonCondition(
								amount.getId(),
								EligibilityReasonOperator.GREATER_THAN_OR_EQUAL_TO,
								new EligibilityReasonConditionScalarValue(new BigDecimal("100.50"))),
						new EligibilityReasonCondition(
								active.getId(),
								EligibilityReasonOperator.EQUAL,
								new EligibilityReasonConditionScalarValue(Boolean.TRUE))),
				"Derek Ochal"
		);

		EligibilityReasonDetail createdEligibilityReason = eligibilityReasonService.create(request);

		assertThat(createdEligibilityReason.getId()).isNotNull();
		assertThat(createdEligibilityReason.getReasonCode()).isEqualTo(request.getReasonCode());
		assertThat(createdEligibilityReason.getReasonName()).isEqualTo(request.getReasonName());
		assertThat(createdEligibilityReason.getConditions()).hasSize(2);
		assertThat(createdEligibilityReason.getFormOptions().getAttributes()).hasSize(2);
		assertThat(createdEligibilityReason.getFormOptions().getAttributes().getFirst().getType())
				.isEqualTo(AccountAttributeType.DECIMAL);
		assertThat(createdEligibilityReason.getFormOptions().getAttributes().get(1).getType())
				.isEqualTo(AccountAttributeType.BOOLEAN);
		assertThat(eligibilityReasonRepository.findById(createdEligibilityReason.getId())).isPresent();
	}

	@Test
	void updatesEligibilityReasonAndReplacesConditions() {
		AccountAttributeEntity openDate = saveAttribute("ATTR0001", "Open Date", AccountAttributeType.DATE);
		AccountAttributeEntity active = saveAttribute("ATTR0002", "Active", AccountAttributeType.BOOLEAN);
		EligibilityReasonDetail createdEligibilityReason = eligibilityReasonService.create(
				new EligibilityReasonRequest(
						"ELIG0001",
						"Initial",
						List.of(new EligibilityReasonCondition(
								openDate.getId(),
								EligibilityReasonOperator.EQUAL,
								new EligibilityReasonConditionScalarValue("2026-01-01"))),
						"Derek Ochal"
				));
		EligibilityReasonRequest updateRequest = new EligibilityReasonRequest(
				"ELIG0001",
				"Updated",
				List.of(new EligibilityReasonCondition(
						active.getId(),
						EligibilityReasonOperator.EQUAL,
						new EligibilityReasonConditionScalarValue(Boolean.FALSE))),
				"Jane Smith"
		);

		EligibilityReasonDetail updatedEligibilityReason = eligibilityReasonService
				.update(createdEligibilityReason.getId(), updateRequest)
				.orElseThrow();

		assertThat(updatedEligibilityReason.getReasonName()).isEqualTo("Updated");
		assertThat(updatedEligibilityReason.getConditions()).hasSize(1);
		assertThat(updatedEligibilityReason.getConditions().getFirst().getAttribute()).isEqualTo(active.getId());
		assertThat(updatedEligibilityReason.getUpdatedBy()).isEqualTo("Jane Smith");
	}

	@Test
	void deletesStoredEligibilityReason() {
		AccountAttributeEntity amount = saveAttribute("ATTR0001", "Min Amount", AccountAttributeType.INTEGER);
		EligibilityReasonDetail createdEligibilityReason = eligibilityReasonService.create(
				new EligibilityReasonRequest(
						"ELIG0001",
						"Min. Balance",
						List.of(new EligibilityReasonCondition(
								amount.getId(),
								EligibilityReasonOperator.GREATER_THAN,
								new EligibilityReasonConditionScalarValue(new BigDecimal("10")))),
						"Derek Ochal"
				));

		boolean deleted = eligibilityReasonService.delete(createdEligibilityReason.getId());

		assertThat(deleted).isTrue();
		assertThat(eligibilityReasonRepository.findById(createdEligibilityReason.getId())).isEmpty();
	}

	private AccountAttributeEntity saveAttribute(String code, String name, AccountAttributeType type) {
		return accountAttributeRepository.save(new AccountAttributeEntity(
				null,
				code,
				name,
				type,
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
	}
}
