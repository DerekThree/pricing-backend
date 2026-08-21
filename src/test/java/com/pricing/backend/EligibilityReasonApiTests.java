package com.pricing.backend;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonConditionEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.FeeType;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanEntity;
import com.pricing.backend.pricingplan.PricingPlanFeeEntity;
import com.pricing.backend.pricingplan.PricingPlanFeeId;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.product.ProductRepository;
import com.pricing.backend.region.RegionEntity;
import com.pricing.backend.region.RegionRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.simulator.SimulatorDateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class EligibilityReasonApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

	@Autowired
	private AccountAttributeRepository accountAttributeRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private SimulatorDateService simulatorDateService;

	@MockitoBean
	private Clock clock;

	@BeforeEach
	void setUp() {
		when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		when(clock.instant()).thenReturn(Instant.parse("2026-08-11T00:00:00Z"));
		simulatorDateService.setCurrentDate(LocalDate.of(2026, 8, 11));
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		feeRepository.deleteAll();
		regionRepository.deleteAll();
		productRepository.deleteAll();
		accountAttributeRepository.deleteAll();
	}

	@Test
	void createsEligibilityReasonFromJsonAndListsIt() throws Exception {
		AccountAttributeEntity amount = saveAttribute("ATTR0001", "Min Amount", AttributeType.DECIMAL);
		AccountAttributeEntity active = saveAttribute("ATTR0002", "Active", AttributeType.BOOLEAN);

		mockMvc.perform(post("/eligibility-reasons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reasonCode": "ELIG0001",
								  "reasonName": "Min. Balance",
								  "conditions": [
								    {"attributeId": %d, "operator": ">=", "value": 100.5},
								    {"attributeId": %d, "operator": "=", "value": true}
								  ],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(amount.getId(), active.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reasonCode").value("ELIG0001"))
				.andExpect(jsonPath("$.reasonName").value("Min. Balance"))
				.andExpect(jsonPath("$.conditions", hasSize(2)))
				.andExpect(jsonPath("$.conditions[0].attributeId").value(amount.getId()))
				.andExpect(jsonPath("$.conditions[0].operator").value(">="))
				.andExpect(jsonPath("$.conditions[0].value").value(100.5))
				.andExpect(jsonPath("$.conditions[1].attributeId").value(active.getId()))
				.andExpect(jsonPath("$.conditions[1].operator").value("="))
				.andExpect(jsonPath("$.conditions[1].value").value(true))
				.andExpect(jsonPath("$.recordOptions.attributes[0].id").value(amount.getId()))
				.andExpect(jsonPath("$.recordOptions.attributes[0].code").value("ATTR0001"))
				.andExpect(jsonPath("$.recordOptions.attributes[0].type").value("DECIMAL"))
				.andExpect(jsonPath("$.recordOptions.attributes[0].productTypes[0]").value("DEPOSIT"))
				.andExpect(jsonPath("$.recordOptions.attributes[1].id").value(active.getId()))
				.andExpect(jsonPath("$.recordOptions.attributes[1].code").value("ATTR0002"))
				.andExpect(jsonPath("$.recordOptions.attributes[1].type").value("BOOLEAN"))
				.andExpect(jsonPath("$.recordOptions.attributes[1].productTypes[0]").value("DEPOSIT"))
				.andExpect(jsonPath("$.formOptions").doesNotExist());

		mockMvc.perform(get("/eligibility-reasons"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].eligibilityReason").value("ELIG0001 - Min. Balance"))
				.andExpect(jsonPath("$[0].conditions", contains("ATTR0001 >= 100.5", "ATTR0002 = true")));
	}

	@Test
	void allowsConditionsWithSameAttributeAndOperator() throws Exception {
		AccountAttributeEntity amount = saveAttribute("ATTR0001", "Min Amount", AttributeType.DECIMAL);

		mockMvc.perform(post("/eligibility-reasons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reasonCode": "ELIG0001",
								  "reasonName": "Balance Range",
								  "conditions": [
								    {"attributeId": %d, "operator": ">=", "value": 100.5},
								    {"attributeId": %d, "operator": ">=", "value": 200.5}
								  ],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(amount.getId(), amount.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.conditions", hasSize(2)))
				.andExpect(jsonPath("$.conditions[0].attributeId").value(amount.getId()))
				.andExpect(jsonPath("$.conditions[0].operator").value(">="))
				.andExpect(jsonPath("$.conditions[0].value").value(100.5))
				.andExpect(jsonPath("$.conditions[1].attributeId").value(amount.getId()))
				.andExpect(jsonPath("$.conditions[1].operator").value(">="))
				.andExpect(jsonPath("$.conditions[1].value").value(200.5));
	}

	@Test
	void returnsEligibilityReasonOptions() throws Exception {
		saveAttribute("ATTR0001", "Min Amount", AttributeType.INTEGER);
		saveAttribute("ATTR0002", "Active", AttributeType.BOOLEAN);

		mockMvc.perform(get("/eligibility-reasons/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attributes", hasSize(2)))
				.andExpect(jsonPath("$.attributes[0].code").value("ATTR0001"))
				.andExpect(jsonPath("$.attributes[0].type").value("INTEGER"))
				.andExpect(jsonPath("$.attributes[0].productTypes[0]").value("DEPOSIT"))
				.andExpect(jsonPath("$.attributes[1].code").value("ATTR0002"))
				.andExpect(jsonPath("$.attributes[1].type").value("BOOLEAN"))
				.andExpect(jsonPath("$.attributes[1].productTypes[0]").value("DEPOSIT"));
	}

	@Test
	void retrievesEligibilityReasonWithAttributeProductTypes() throws Exception {
		AccountAttributeEntity attribute = saveAttribute("ATTR0001", "Active", AttributeType.BOOLEAN);
		EligibilityReasonEntity reason = saveReasonWithCondition("ELIG0001", attribute);

		mockMvc.perform(get("/eligibility-reasons/{id}", reason.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recordOptions.attributes[0].productTypes[0]").value("DEPOSIT"));
	}

	@Test
	void rejectsEligibilityReasonWhenConditionAttributesHaveNoCommonProductType() throws Exception {
		AccountAttributeEntity deposit = saveAttribute(
				"ATTR0001", "Min Amount", AttributeType.DECIMAL, ProductType.DEPOSIT);
		AccountAttributeEntity credit = saveAttribute(
				"ATTR0002", "Active", AttributeType.BOOLEAN, ProductType.CREDIT);

		mockMvc.perform(post("/eligibility-reasons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reasonCode": "ELIG0001",
								  "reasonName": "Min. Balance",
								  "conditions": [
								    {"attributeId": %d, "operator": ">=", "value": 100.5},
								    {"attributeId": %d, "operator": "=", "value": true}
								  ],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(deposit.getId(), credit.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Applicable Product Types must contain at least one item"));
	}

	@Test
	void allowsReasonNameAndCodeUpdatesAndRejectsConditionChangesWhenAnyPricingPlanReferencesIt()
			throws Exception {
		ProductEntity product = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity region = regionRepository.save(RegionEntity.builder()
				.regionCode("REG00001")
				.regionName("Midwest")
				.states(List.of())
				.zipCodes(List.of())
				.branches(List.of())
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		FeeEntity fee = feeRepository.save(FeeEntity.builder()
				.feeCode("FEE00001")
				.feeName("Monthly Maintenance Fee")
				.feeType(FeeType.FLAT)
				.productTypes(List.of(ProductType.DEPOSIT))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		AccountAttributeEntity attribute = saveAttribute("ATTR0001", "Active", AttributeType.BOOLEAN);
		EligibilityReasonEntity past = saveReason("ELIG0001");
		EligibilityReasonEntity scheduled = saveReason("ELIG0002");
		EligibilityReasonEntity active = saveReason("ELIG0003");
		EligibilityReasonEntity mixed = saveReason("ELIG0004");
		savePricingPlan("PLAN0001", product, region, fee, "2026-08-01", "2026-08-10", past, mixed);
		savePricingPlan("PLAN0002", product, region, fee, "2026-08-11", "2026-08-11", active, mixed);
		savePricingPlan("PLAN0003", product, region, fee, "2026-08-12", "2026-08-20", scheduled);

		mockMvc.perform(put("/eligibility-reasons/{id}", past.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestJson("ELIG0001", "Updated Past")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reasonName").value("Updated Past"));
		mockMvc.perform(put("/eligibility-reasons/{id}", past.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestJson("ELIG9998", "Updated Past")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reasonCode").value("ELIG9998"));
		mockMvc.perform(put("/eligibility-reasons/{id}", scheduled.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestJson("ELIG9999", "Updated Scheduled")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reasonCode").value("ELIG9999"));
		mockMvc.perform(put("/eligibility-reasons/{id}", scheduled.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestWithConditionJson("ELIG9999", "Updated Scheduled", attribute.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This eligibility reason is used by pricing plan with code PLAN0003. "
								+ "Please update the pricing plan first."));
		mockMvc.perform(put("/eligibility-reasons/{id}", active.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestJson("ELIG0003", "Updated Active")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reasonName").value("Updated Active"));
		mockMvc.perform(put("/eligibility-reasons/{id}", active.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestJson("ELIG9997", "Updated Active")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reasonCode").value("ELIG9997"));
		mockMvc.perform(put("/eligibility-reasons/{id}", active.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestWithConditionJson("ELIG9997", "Updated Active", attribute.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This eligibility reason is used by pricing plan with code PLAN0002. "
								+ "Please update the pricing plan first."));
		mockMvc.perform(put("/eligibility-reasons/{id}", mixed.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(reasonRequestWithConditionJson("ELIG0004", "Updated Mixed", attribute.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This eligibility reason is used by pricing plan with code PLAN0001. "
								+ "Please update the pricing plan first."));
	}

	@Test
	void allowsAttributeNameAndCodeUpdatesAndRejectsDefinitionChangesWhenAnEligibilityReasonUsesIt()
			throws Exception {
		ProductEntity product = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity region = regionRepository.save(RegionEntity.builder()
				.regionCode("REG00001")
				.regionName("Midwest")
				.states(List.of())
				.zipCodes(List.of())
				.branches(List.of())
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		FeeEntity fee = feeRepository.save(FeeEntity.builder()
				.feeCode("FEE00001")
				.feeName("Monthly Maintenance Fee")
				.feeType(FeeType.FLAT)
				.productTypes(List.of(ProductType.DEPOSIT))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		AccountAttributeEntity past = saveAttribute("ATTR0001", "Past", AttributeType.BOOLEAN);
		AccountAttributeEntity active = saveAttribute("ATTR0002", "Active", AttributeType.BOOLEAN);
		AccountAttributeEntity scheduled = saveAttribute("ATTR0003", "Scheduled", AttributeType.BOOLEAN);
		EligibilityReasonEntity pastReason = saveReasonWithCondition("ELIG0001", past);
		EligibilityReasonEntity activeReason = saveReasonWithCondition("ELIG0002", active);
		EligibilityReasonEntity scheduledReason = saveReasonWithCondition("ELIG0003", scheduled);
		savePricingPlan("PLAN0001", product, region, fee, "2026-08-01", "2026-08-10", pastReason);
		savePricingPlan("PLAN0002", product, region, fee, "2026-08-11", "2026-08-11", activeReason);
		savePricingPlan("PLAN0003", product, region, fee, "2026-08-12", "2026-08-20", scheduledReason);

		mockMvc.perform(put("/account-attributes/{id}", past.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("ATTR0001", "Updated Past", "BOOLEAN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attributeName").value("Updated Past"));
		mockMvc.perform(put("/account-attributes/{id}", past.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("ATTR9998", "Updated Past", "BOOLEAN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attributeCode").value("ATTR9998"));
		mockMvc.perform(put("/account-attributes/{id}", active.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("ATTR0002", "Updated Active", "INTEGER")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This account attribute is used by eligibility reason with code ELIG0002. "
								+ "Please update the eligibility reason first."));
		mockMvc.perform(put("/account-attributes/{id}", scheduled.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("ATTR0003", "Updated Scheduled", "INTEGER")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This account attribute is used by eligibility reason with code ELIG0003. "
								+ "Please update the eligibility reason first."));
	}

	private EligibilityReasonEntity saveReason(String code) {
		return eligibilityReasonRepository.save(EligibilityReasonEntity.builder()
				.reasonCode(code)
				.reasonName("Reason " + code)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
	}

	private EligibilityReasonEntity saveReasonWithCondition(String code, AccountAttributeEntity attribute) {
		EligibilityReasonEntity reason = saveReason(code);
		reason.getConditions().add(EligibilityReasonConditionEntity.builder()
				.reason(reason)
				.attribute(attribute)
				.operator("=")
				.attributeValue("true")
				.build());
		return eligibilityReasonRepository.saveAndFlush(reason);
	}

	private void savePricingPlan(String code, ProductEntity product, RegionEntity region,
			FeeEntity fee,
			String activeFrom, String activeThrough, EligibilityReasonEntity... reasons) {
		PricingPlanEntity pricingPlan = PricingPlanEntity.builder()
				.planCode(code)
				.planName("Plan " + code)
				.product(product)
				.region(region)
				.activeFrom(LocalDate.parse(activeFrom))
				.activeThrough(LocalDate.parse(activeThrough))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build();
		PricingPlanFeeEntity pricingPlanFee = new PricingPlanFeeEntity();
		pricingPlanFee.setId(new PricingPlanFeeId(null, fee.getId()));
		pricingPlanFee.setPricingPlan(pricingPlan);
		pricingPlanFee.setFee(fee);
		pricingPlanFee.setAmount(BigDecimal.ONE);
		pricingPlanFee.setReasons(List.of(reasons));
		pricingPlan.getFees().add(pricingPlanFee);
		pricingPlanRepository.saveAndFlush(pricingPlan);
	}

	private String reasonRequestJson(String code, String name) {
		return """
				{
				  "reasonCode": "%s",
				  "reasonName": "%s",
				  "conditions": [],
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(code, name);
	}

	private String reasonRequestWithConditionJson(String code, String name, Long attributeId) {
		return """
				{
				  "reasonCode": "%s",
				  "reasonName": "%s",
				  "conditions": [{"attributeId": %d, "operator": "=", "value": true}],
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(code, name, attributeId);
	}

	private String attributeRequestJson(String code, String name, String type) {
		return """
				{
				  "attributeCode": "%s",
				  "attributeName": "%s",
				  "attributeType": "%s",
				  "productTypes": ["DEPOSIT"],
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(code, name, type);
	}

	private AccountAttributeEntity saveAttribute(String code, String name, AttributeType type) {
		return saveAttribute(code, name, type, ProductType.DEPOSIT);
	}

	private AccountAttributeEntity saveAttribute(
			String code, String name, AttributeType type, ProductType... productTypes) {
		return accountAttributeRepository.save(new AccountAttributeEntity(
				null,
				code,
				name,
				type,
				List.of(productTypes),
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
	}
}
