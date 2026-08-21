package com.pricing.backend;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonConditionEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonConditionId;
import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.FeeType;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanEntity;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.product.ProductRepository;
import com.pricing.backend.region.RegionEntity;
import com.pricing.backend.region.RegionRepository;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PricingPlanApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

	@Autowired
	private AccountAttributeRepository accountAttributeRepository;

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
		accountAttributeRepository.deleteAll();
		feeRepository.deleteAll();
		regionRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void createsPricingPlanWithZeroFeeAmount() throws Exception {
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
		EligibilityReasonEntity reason = eligibilityReasonRepository.save(EligibilityReasonEntity.builder()
				.reasonCode("ELIG0001")
				.reasonName("Min. Balance")
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());

		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "planCode": "PLAN0001",
								  "planName": "Premier Midwest",
								  "productId": %d,
								  "regionId": %d,
								  "activeFrom": "2026-08-11",
								  "activeThrough": "2026-12-31",
								  "fees": [
							    {"feeId": %d, "amount": 0, "reasonIds": [%d]}
								  ],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(product.getId(), region.getId(), fee.getId(), reason.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.planCode").value("PLAN0001"))
				.andExpect(jsonPath("$.productId").value(product.getId()))
				.andExpect(jsonPath("$.regionId").value(region.getId()))
				.andExpect(jsonPath("$.fees", hasSize(1)))
				.andExpect(jsonPath("$.fees[0].feeId").value(fee.getId()))
				.andExpect(jsonPath("$.fees[0].amount").value(0))
				.andExpect(jsonPath("$.fees[0].reasonIds[0]").value(reason.getId()))
				.andExpect(jsonPath("$.product").doesNotExist())
				.andExpect(jsonPath("$.region").doesNotExist())
				.andExpect(jsonPath("$.fees[0].fee").doesNotExist())
				.andExpect(jsonPath("$.recordOptions.products[0].id").value(product.getId()))
				.andExpect(jsonPath("$.recordOptions.regions[0].id").value(region.getId()))
				.andExpect(jsonPath("$.recordOptions.fees[0].id").value(fee.getId()))
				.andExpect(jsonPath("$.recordOptions.reasons[0].id").value(reason.getId()))
				.andExpect(jsonPath("$.recordOptions.reasons[0].productTypes",
						contains("DEPOSIT", "CD", "CREDIT")))
				.andExpect(jsonPath("$.recordOptions.currentDate").doesNotExist())
				.andExpect(jsonPath("$.recordOptions.intervals").doesNotExist())
				.andExpect(jsonPath("$.formOptions").doesNotExist());

		Long pricingPlanId = pricingPlanRepository.findAllByOrderByPlanCodeAsc().getFirst().getId();
		mockMvc.perform(get("/pricing-plans/{id}", pricingPlanId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fees", hasSize(1)))
				.andExpect(jsonPath("$.fees[0].reasonIds", contains(reason.getId().intValue())));
	}

	@Test
	void allowsSameFeeOnMultiplePricingPlans() throws Exception {
		ProductEntity firstProduct = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		ProductEntity secondProduct = productRepository.save(ProductEntity.builder()
				.productCode("PROD0002")
				.productName("Student Checking")
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
				.feeName("Overdraft Fee")
				.feeType(FeeType.FLAT)
				.productTypes(List.of(ProductType.DEPOSIT))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());

		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanRequestJson("PLAN0001", firstProduct.getId(), region.getId(), fee.getId())))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanRequestJson("PLAN0002", secondProduct.getId(), region.getId(), fee.getId())))
				.andExpect(status().isCreated());
	}

	@Test
	void rejectsDuplicateFeesAndInvalidReferences() throws Exception {
		ProductEntity product = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity region = regionRepository.save(region("REG00001", "Midwest"));
		FeeEntity fee = feeRepository.save(fee("FEE00001", ProductType.DEPOSIT));
		EligibilityReasonEntity reason = eligibilityReasonRepository.save(
				EligibilityReasonEntity.builder()
						.reasonCode("ELIG0001")
						.reasonName("Min. Balance")
						.updatedBy("Derek Ochal")
						.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
						.build());

		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "planCode": "PLAN0001",
								  "planName": "Test Plan",
								  "productId": %d,
								  "regionId": %d,
								  "activeFrom": "2026-08-11",
								  "activeThrough": "2026-12-31",
								  "fees": [
								    {"feeId": %d, "amount": 7.50, "reasonIds": []},
								    {"feeId": %d, "amount": 7.50, "reasonIds": []}
								  ],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(product.getId(), region.getId(), fee.getId(), fee.getId())))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "planCode": "PLAN0002",
								  "planName": "Test Plan",
								  "productId": %d,
								  "regionId": %d,
								  "activeFrom": "2026-08-11",
								  "activeThrough": "2026-12-31",
								  "fees": [
								    {"feeId": %d, "amount": 7.50, "reasonIds": [%d, %d]}
								  ],
								  "updatedBy": "Derek Ochal"
								}
				""".formatted(product.getId(), region.getId(), fee.getId(), reason.getId(),
						reason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Test Plan", product.getId(), region.getId(),
								"2026-08-11", "2026-12-31", fee.getId(), "7.50", 999_999L)))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0004", "Test Plan", product.getId(), 999_999L,
								"2026-08-11", "2026-12-31", fee.getId(), "7.50", reason.getId())))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectsReasonOutsidePricingPlanProductTypeOnCreateAndUpdate() throws Exception {
		ProductEntity product = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity region = regionRepository.save(region("REG00001", "Midwest"));
		FeeEntity fee = feeRepository.save(fee("FEE00001", ProductType.DEPOSIT));
		AccountAttributeEntity attribute = accountAttributeRepository.save(
				attribute("ATTR0001", ProductType.CREDIT));
		EligibilityReasonEntity reason = eligibilityReasonRepository.save(
				EligibilityReasonEntity.builder()
						.reasonCode("ELIG0001")
						.reasonName("Credit Score")
						.updatedBy("Derek Ochal")
						.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
						.build());
		reason.getConditions().add(condition(reason, attribute));
		eligibilityReasonRepository.save(reason);
		String request = pricingPlanFeeRequestJson(
				"PLAN0001", "Test Plan", product.getId(), region.getId(), "2026-08-12", "2026-12-31",
				fee.getId(), "7.50", reason.getId());

		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Eligibility reason with code ELIG0001 cannot be used for this product type"));

		PricingPlanEntity scheduled = pricingPlanRepository.save(pricingPlan(
				"PLAN0001", product, region, "2026-08-12", "2026-12-31"));
		mockMvc.perform(put("/pricing-plans/{id}", scheduled.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Eligibility reason with code ELIG0001 cannot be used for this product type"));
	}

	@Test
	void enforcesActivePeriodBoundariesAndAllowsDifferentPairs() throws Exception {
		ProductEntity firstProduct = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		ProductEntity secondProduct = productRepository.save(ProductEntity.builder()
				.productCode("PROD0002")
				.productName("Student Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity firstRegion = regionRepository.save(region("REG00001", "Midwest"));
		RegionEntity secondRegion = regionRepository.save(region("REG00002", "Northeast"));
		PricingPlanEntity existing = pricingPlanRepository.save(pricingPlan("PLAN0001", firstProduct,
				firstRegion, "2026-08-20", "2026-08-30"));

		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0002", firstProduct.getId(), firstRegion.getId(),
								"2026-08-10", "2026-08-10")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0003", firstProduct.getId(), firstRegion.getId(),
								"2026-08-11", "2026-08-11")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0004", firstProduct.getId(), firstRegion.getId(),
								"2026-09-02", "2026-09-01")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Active Through must be on or after Active From"));
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0005", firstProduct.getId(), firstRegion.getId(),
								"2026-08-31", "2026-09-01")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0006", secondProduct.getId(), firstRegion.getId(),
								"2026-08-20", "2026-08-30")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0007", firstProduct.getId(), secondRegion.getId(),
								"2026-08-20", "2026-08-30")))
				.andExpect(status().isCreated());
		mockMvc.perform(put("/pricing-plans/{id}", existing.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0001", firstProduct.getId(), firstRegion.getId(),
								"2026-08-20", "2026-08-30")))
				.andExpect(status().isOk());
	}

	@Test
	void enforcesPricingPlanLifecycleUpdateRules() throws Exception {
		ProductEntity firstProduct = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		ProductEntity secondProduct = productRepository.save(ProductEntity.builder()
				.productCode("PROD0002")
				.productName("Student Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity firstRegion = regionRepository.save(region("REG00001", "Midwest"));
		RegionEntity secondRegion = regionRepository.save(region("REG00002", "Northeast"));
		FeeEntity firstFee = feeRepository.save(fee("FEE00001", ProductType.DEPOSIT));
		FeeEntity secondFee = feeRepository.save(fee("FEE00002", ProductType.DEPOSIT));
		EligibilityReasonEntity firstReason = eligibilityReasonRepository.save(EligibilityReasonEntity.builder()
				.reasonCode("ELIG0001")
				.reasonName("Min. Balance")
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		EligibilityReasonEntity secondReason = eligibilityReasonRepository.save(EligibilityReasonEntity.builder()
				.reasonCode("ELIG0002")
				.reasonName("Student")
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		PricingPlanEntity scheduled = pricingPlanRepository.save(pricingPlan("PLAN0001", firstProduct,
				firstRegion, "2026-08-12", "2026-08-20"));

		mockMvc.perform(put("/pricing-plans/{id}", scheduled.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0002", "Updated scheduled plan", secondProduct.getId(),
								secondRegion.getId(), "2026-08-13", "2026-08-20", secondFee.getId(), "8.50",
								secondReason.getId())))
				.andExpect(status().isOk());

		mockMvc.perform(post("/pricing-plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Active plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-20", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isCreated());
		PricingPlanEntity activeAtStart = pricingPlanRepository.findAll().stream()
				.filter(plan -> plan.getPlanCode().equals("PLAN0003"))
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated active plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isOk());

		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN9999", "Updated active plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated active plan", secondProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated active plan", firstProduct.getId(),
								secondRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated active plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-10", "2026-08-11", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated active plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", secondFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated active plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "8.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated active plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "7.50",
								secondReason.getId())))
				.andExpect(status().isBadRequest());

		PricingPlanEntity active = pricingPlanRepository.save(pricingPlan("PLAN0004", firstProduct,
				secondRegion, "2026-08-01", "2026-08-20"));
		mockMvc.perform(put("/pricing-plans/{id}", active.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanPeriodRequestJson("PLAN0004", firstProduct.getId(), secondRegion.getId(),
								"2026-08-01", "2026-08-10")))
				.andExpect(status().isBadRequest());

		simulatorDateService.setCurrentDate(LocalDate.of(2026, 8, 12));
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated past plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isOk());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated past plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-11", firstFee.getId(), "8.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/pricing-plans/{id}", activeAtStart.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(pricingPlanFeeRequestJson("PLAN0003", "Updated past plan", firstProduct.getId(),
								firstRegion.getId(), "2026-08-11", "2026-08-12", firstFee.getId(), "7.50",
								firstReason.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void allowsScheduledAndPastPricingPlanDeletionButRejectsActiveDeletion() throws Exception {
		ProductEntity product = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity region = regionRepository.save(region("REG00001", "Midwest"));
		PricingPlanEntity scheduled = pricingPlanRepository.save(pricingPlan("PLAN0001", product, region,
				"2026-08-12", "2026-08-20"));
		PricingPlanEntity active = pricingPlanRepository.save(pricingPlan("PLAN0002", product, region,
				"2026-08-11", "2026-08-11"));
		PricingPlanEntity past = pricingPlanRepository.save(pricingPlan("PLAN0003", product, region,
				"2026-08-01", "2026-08-10"));

		mockMvc.perform(delete("/pricing-plans/{id}", scheduled.getId()))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/pricing-plans/{id}", active.getId()))
				.andExpect(status().isBadRequest());
		mockMvc.perform(delete("/pricing-plans/{id}", past.getId()))
				.andExpect(status().isNoContent());
	}

	@Test
	void returnsPricingPlanOptionsForTheSelectedProductAndRegion() throws Exception {
		ProductEntity checking = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		ProductEntity loan = productRepository.save(ProductEntity.builder()
				.productCode("PROD0002")
				.productName("Personal Credit")
				.productType(ProductType.CREDIT)
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		RegionEntity midwest = regionRepository.save(region("REG00001", "Midwest"));
		RegionEntity northeast = regionRepository.save(region("REG00002", "Northeast"));
		FeeEntity maintenanceFee = feeRepository.save(fee("FEE00001", ProductType.DEPOSIT));
		feeRepository.save(fee("FEE00002", ProductType.CREDIT));
		AccountAttributeEntity firstAttribute = accountAttributeRepository.save(
				attribute("ATTR0001", ProductType.DEPOSIT, ProductType.CD));
		AccountAttributeEntity secondAttribute = accountAttributeRepository.save(
				attribute("ATTR0002", ProductType.DEPOSIT, ProductType.CREDIT));
		EligibilityReasonEntity reason = eligibilityReasonRepository.save(
				EligibilityReasonEntity.builder()
						.reasonCode("ELIG0001")
						.reasonName("Min. Balance")
						.updatedBy("Derek Ochal")
						.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
						.build());
		reason.getConditions().add(condition(reason, firstAttribute));
		reason.getConditions().add(condition(reason, secondAttribute));
		eligibilityReasonRepository.save(reason);
		PricingPlanEntity active = pricingPlanRepository.save(pricingPlan("PLAN0001", checking, midwest,
					"2026-08-11", "2026-12-31"));
		pricingPlanRepository.save(pricingPlan("PLAN0002", checking, midwest, "2026-09-01", "2027-08-31"));
		pricingPlanRepository.save(pricingPlan("PLAN0003", checking, midwest, "2026-01-01", "2026-08-10"));
		pricingPlanRepository.save(pricingPlan("PLAN0004", checking, northeast, "2026-08-11", "2026-12-31"));
		pricingPlanRepository.save(pricingPlan("PLAN0005", loan, midwest, "2026-08-11", "2026-12-31"));

		mockMvc.perform(get("/pricing-plans/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentDate").value("2026-08-11"))
				.andExpect(jsonPath("$.products", hasSize(2)))
				.andExpect(jsonPath("$.regions", hasSize(2)))
				.andExpect(jsonPath("$.productId").doesNotExist())
				.andExpect(jsonPath("$.regionId").doesNotExist())
				.andExpect(jsonPath("$.fees").doesNotExist())
				.andExpect(jsonPath("$.reasons").doesNotExist())
				.andExpect(jsonPath("$.intervals").doesNotExist());

		mockMvc.perform(get("/pricing-plans/options").param("recordId", String.valueOf(active.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(checking.getId()))
				.andExpect(jsonPath("$.regionId").value(midwest.getId()))
				.andExpect(jsonPath("$.fees[*].id", contains(maintenanceFee.getId().intValue())))
				.andExpect(jsonPath("$.reasons[*].id", contains(reason.getId().intValue())))
				.andExpect(jsonPath("$.reasons[0].productTypes", contains("DEPOSIT")))
				.andExpect(jsonPath("$.intervals[*].activeFrom",
						contains("2026-09-01")));

		mockMvc.perform(get("/pricing-plans/options/secondary")
						.param("productId", String.valueOf(checking.getId()))
						.param("regionId", String.valueOf(midwest.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(checking.getId()))
				.andExpect(jsonPath("$.regionId").value(midwest.getId()))
				.andExpect(jsonPath("$.fees[*].id", contains(maintenanceFee.getId().intValue())))
				.andExpect(jsonPath("$.reasons[0].productTypes", contains("DEPOSIT")))
				.andExpect(jsonPath("$.intervals[*].activeFrom",
						containsInAnyOrder("2026-08-11", "2026-09-01")));

		mockMvc.perform(get("/pricing-plans/options/secondary"))
				.andExpect(status().isBadRequest());
	}

	private RegionEntity region(String code, String name) {
		return RegionEntity.builder()
				.regionCode(code)
				.regionName(name)
				.states(List.of())
				.zipCodes(List.of())
				.branches(List.of())
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build();
	}

	private FeeEntity fee(String code, ProductType productType) {
		return FeeEntity.builder()
				.feeCode(code)
				.feeName("Fee " + code)
				.feeType(FeeType.FLAT)
				.productTypes(List.of(productType))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build();
	}

	private AccountAttributeEntity attribute(String code, ProductType... productTypes) {
		return AccountAttributeEntity.builder()
				.attributeCode(code)
				.attributeName("Attribute " + code)
				.attributeType(AttributeType.DECIMAL)
				.productTypes(List.of(productTypes))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build();
	}

	private EligibilityReasonConditionEntity condition(
			EligibilityReasonEntity reason, AccountAttributeEntity attribute) {
		return EligibilityReasonConditionEntity.builder()
				.id(new EligibilityReasonConditionId(reason.getId(), attribute.getId(), "="))
				.reason(reason)
				.attribute(attribute)
				.attributeValue("10")
				.build();
	}

	private PricingPlanEntity pricingPlan(String code, ProductEntity product, RegionEntity region,
			String activeFrom, String activeThrough) {
		return PricingPlanEntity.builder()
				.planCode(code)
				.planName("Plan " + code)
				.product(product)
				.region(region)
				.activeFrom(LocalDate.parse(activeFrom))
				.activeThrough(LocalDate.parse(activeThrough))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build();
	}

	private String pricingPlanRequestJson(String planCode, Long productId, Long regionId, Long feeId) {
		return """
				{
				  "planCode": "%s",
				  "planName": "Test Plan",
				  "productId": %d,
				  "regionId": %d,
				  "activeFrom": "2026-08-11",
				  "activeThrough": "2026-12-31",
				  "fees": [
				    {"feeId": %d, "amount": 7.50, "reasonIds": []}
				  ],
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(planCode, productId, regionId, feeId);
	}

	private String pricingPlanPeriodRequestJson(String planCode, Long productId, Long regionId,
			String activeFrom, String activeThrough) {
		return """
				{
				  "planCode": "%s",
				  "planName": "Test Plan",
				  "productId": %d,
				  "regionId": %d,
				  "activeFrom": "%s",
				  "activeThrough": "%s",
				  "fees": [],
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(planCode, productId, regionId, activeFrom, activeThrough);
	}

	private String pricingPlanFeeRequestJson(String planCode, String planName, Long productId, Long regionId,
			String activeFrom, String activeThrough, Long feeId, String amount, Long reasonId) {
		return """
				{
				  "planCode": "%s",
				  "planName": "%s",
				  "productId": %d,
				  "regionId": %d,
				  "activeFrom": "%s",
				  "activeThrough": "%s",
				  "fees": [
				    {"feeId": %d, "amount": %s, "reasonIds": [%d]}
				  ],
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(planCode, planName, productId, regionId, activeFrom, activeThrough, feeId, amount,
						reasonId);
	}
}
