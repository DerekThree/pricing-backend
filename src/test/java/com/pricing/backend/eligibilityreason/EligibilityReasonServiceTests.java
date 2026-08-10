package com.pricing.backend.eligibilityreason;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.ReasonCondition;
import com.pricing.backend.generated.model.ReasonDetail;
import com.pricing.backend.generated.model.ReasonOperator;
import com.pricing.backend.generated.model.ReasonRequest;
import com.pricing.backend.pricingplan.PricingPlanRepository;
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

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
	}

	@Test
	void createsEligibilityReason() {
		AccountAttributeEntity amount = saveAttribute("ATTR0001", "Min Amount", AttributeType.DECIMAL);
		AccountAttributeEntity active = saveAttribute("ATTR0002", "Active", AttributeType.BOOLEAN);
		ReasonRequest request = new ReasonRequest(
				"ELIG0001",
				"Min. Balance",
				List.of(
						new ReasonCondition(
								amount.getId(),
								ReasonOperator.GREATER_THAN_OR_EQUAL_TO,
								new EligibilityReasonConditionScalarValue(new BigDecimal("100.50"))),
						new ReasonCondition(
								active.getId(),
								ReasonOperator.EQUAL,
								new EligibilityReasonConditionScalarValue(Boolean.TRUE))),
				"Derek Ochal"
		);

		ReasonDetail createdEligibilityReason = eligibilityReasonService.create(request);

		assertThat(createdEligibilityReason.getId()).isNotNull();
		assertThat(createdEligibilityReason.getReasonCode()).isEqualTo(request.getReasonCode());
		assertThat(createdEligibilityReason.getReasonName()).isEqualTo(request.getReasonName());
		assertThat(createdEligibilityReason.getConditions()).hasSize(2);
		assertThat(createdEligibilityReason.getConditions().getFirst().getAttributeId())
				.isEqualTo(amount.getId());
		assertThat(createdEligibilityReason.getConditions().get(1).getAttributeId())
				.isEqualTo(active.getId());
		assertThat(eligibilityReasonRepository.findById(createdEligibilityReason.getId())).isPresent();
	}

	@Test
	void updatesEligibilityReasonAndReplacesConditions() {
		AccountAttributeEntity openDate = saveAttribute("ATTR0001", "Open Date", AttributeType.DATE);
		AccountAttributeEntity active = saveAttribute("ATTR0002", "Active", AttributeType.BOOLEAN);
		ReasonDetail createdEligibilityReason = eligibilityReasonService.create(
				new ReasonRequest(
						"ELIG0001",
						"Initial",
						List.of(new ReasonCondition(
								openDate.getId(),
								ReasonOperator.EQUAL,
								new EligibilityReasonConditionScalarValue("2026-01-01"))),
						"Derek Ochal"
				));
		ReasonRequest updateRequest = new ReasonRequest(
				"ELIG0001",
				"Updated",
				List.of(new ReasonCondition(
						active.getId(),
						ReasonOperator.EQUAL,
						new EligibilityReasonConditionScalarValue(Boolean.FALSE))),
				"Jane Smith"
		);

		ReasonDetail updatedEligibilityReason = eligibilityReasonService
				.update(createdEligibilityReason.getId(), updateRequest)
				.orElseThrow();

		assertThat(updatedEligibilityReason.getReasonName()).isEqualTo("Updated");
		assertThat(updatedEligibilityReason.getConditions()).hasSize(1);
		assertThat(updatedEligibilityReason.getConditions().getFirst().getAttributeId())
				.isEqualTo(active.getId());
		assertThat(updatedEligibilityReason.getUpdatedBy()).isEqualTo("Jane Smith");
	}

	@Test
	void deletesStoredEligibilityReason() {
		AccountAttributeEntity amount = saveAttribute("ATTR0001", "Min Amount", AttributeType.INTEGER);
		ReasonDetail createdEligibilityReason = eligibilityReasonService.create(
				new ReasonRequest(
						"ELIG0001",
						"Min. Balance",
						List.of(new ReasonCondition(
								amount.getId(),
								ReasonOperator.GREATER_THAN,
								new EligibilityReasonConditionScalarValue(new BigDecimal("10")))),
						"Derek Ochal"
				));

		boolean deleted = eligibilityReasonService.delete(createdEligibilityReason.getId());

		assertThat(deleted).isTrue();
		assertThat(eligibilityReasonRepository.findById(createdEligibilityReason.getId())).isEmpty();
	}

	private AccountAttributeEntity saveAttribute(String code, String name, AttributeType type) {
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
