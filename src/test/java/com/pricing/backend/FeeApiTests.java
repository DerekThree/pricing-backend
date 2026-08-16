package com.pricing.backend;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
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
import com.pricing.backend.simulator.SimulatorDateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class FeeApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private RegionRepository regionRepository;

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
		feeRepository.deleteAll();
		regionRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void createsFeeAndListsIt() throws Exception {
		mockMvc.perform(post("/fees")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "feeCode": "FEE00001",
								  "feeName": "Monthly Maintenance Fee",
								  "feeType": "FLAT",
								  "productTypes": ["DEPOSIT"],
								  "updatedBy": "Derek Ochal"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.feeCode").value("FEE00001"))
				.andExpect(jsonPath("$.feeName").value("Monthly Maintenance Fee"))
				.andExpect(jsonPath("$.feeType").value("FLAT"))
				.andExpect(jsonPath("$.productTypes[0]").value("DEPOSIT"));

		mockMvc.perform(get("/fees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].fee").value("FEE00001 - Monthly Maintenance Fee"))
				.andExpect(jsonPath("$[0].feeType").value("FLAT"))
				.andExpect(jsonPath("$[0].productTypes[0]").value("DEPOSIT"));
	}

	@Test
	void rejectsEmptyAndDuplicateProductTypes() throws Exception {
		mockMvc.perform(post("/fees")
						.contentType(MediaType.APPLICATION_JSON)
						.content(feeRequestJson("FEE00001", "Monthly Maintenance Fee", "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("productTypes size must be between 1 and 2147483647"));
		mockMvc.perform(post("/fees")
						.contentType(MediaType.APPLICATION_JSON)
						.content(feeRequestJson("FEE00001", "Monthly Maintenance Fee",
								"[\"DEPOSIT\", \"DEPOSIT\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A fee cannot contain the same product type twice"));
	}

	@Test
	void allowsFeeNameUpdatesAndRejectsDefinitionChangesWhenAnyPricingPlanContainsIt()
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
		FeeEntity past = saveFee("FEE00001");
		FeeEntity active = saveFee("FEE00002");
		FeeEntity scheduled = saveFee("FEE00003");
		savePricingPlan("PLAN0001", product, region, past, "2026-08-01", "2026-08-10");
		savePricingPlan("PLAN0002", product, region, active, "2026-08-11", "2026-08-11");
		savePricingPlan("PLAN0003", product, region, scheduled, "2026-08-12", "2026-08-20");

		mockMvc.perform(put("/fees/{id}", past.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(feeRequestJson("FEE00001", "Updated Past", "[\"DEPOSIT\"]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.feeName").value("Updated Past"));
		mockMvc.perform(put("/fees/{id}", past.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(feeRequestJson("FEE9998", "Updated Past", "[\"DEPOSIT\"]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This fee is used by pricing plan with code PLAN0001. "
								+ "Please update the pricing plan first."));
		mockMvc.perform(put("/fees/{id}", active.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(feeRequestJson("FEE00002", "Updated Active", "[\"CREDIT\"]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This fee is used by pricing plan with code PLAN0002. "
								+ "Please update the pricing plan first."));
		mockMvc.perform(put("/fees/{id}", scheduled.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(feeRequestJson("FEE9999", "Updated Scheduled", "[\"CREDIT\"]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This fee is used by pricing plan with code PLAN0003. "
								+ "Please update the pricing plan first."));
	}

	@Test
	void allowsProductTypeReorderingWhenAPricingPlanContainsTheFee() throws Exception {
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
				.productTypes(List.of(ProductType.DEPOSIT, ProductType.CREDIT))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		savePricingPlan("PLAN0001", product, region, fee, "2026-08-12", "2026-08-20");

		mockMvc.perform(put("/fees/{id}", fee.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(feeRequestJson("FEE00001", "Monthly Maintenance Fee",
								"[\"CREDIT\", \"DEPOSIT\"]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productTypes[0]").value("CREDIT"))
				.andExpect(jsonPath("$.productTypes[1]").value("DEPOSIT"));
	}

	private FeeEntity saveFee(String code) {
		return feeRepository.save(FeeEntity.builder()
				.feeCode(code)
				.feeName("Fee " + code)
				.feeType(FeeType.FLAT)
				.productTypes(List.of(ProductType.DEPOSIT))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
	}

	private void savePricingPlan(String code, ProductEntity product, RegionEntity region, FeeEntity fee,
			String activeFrom, String activeThrough) {
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
		pricingPlanFee.setReasons(List.of());
		pricingPlan.getFees().add(pricingPlanFee);
		pricingPlanRepository.saveAndFlush(pricingPlan);
	}

	private String feeRequestJson(String code, String name, String productTypes) {
		return """
				{
				  "feeCode": "%s",
				  "feeName": "%s",
				  "feeType": "FLAT",
				  "productTypes": %s,
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(code, name, productTypes);
	}
}
