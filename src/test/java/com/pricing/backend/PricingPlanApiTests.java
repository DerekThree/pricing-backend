package com.pricing.backend;

import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.FeeType;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.product.ProductRepository;
import com.pricing.backend.region.RegionEntity;
import com.pricing.backend.region.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		feeRepository.deleteAll();
		regionRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void createsPricingPlanWithEmbeddedProductRegionAndFees() throws Exception {
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
								  "activeFrom": "2026-01-01",
								  "activeThrough": "2026-12-31",
								  "fees": [
								    {"feeId": %d, "amount": 7.50, "reasons": [%d]}
								  ],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(product.getId(), region.getId(), fee.getId(), reason.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.planCode").value("PLAN0001"))
				.andExpect(jsonPath("$.product.id").value(product.getId()))
				.andExpect(jsonPath("$.product.code").value("PROD0001"))
				.andExpect(jsonPath("$.product.type").value("DEPOSIT"))
				.andExpect(jsonPath("$.region.id").value(region.getId()))
				.andExpect(jsonPath("$.region.code").value("REG00001"))
				.andExpect(jsonPath("$.fees", hasSize(1)))
				.andExpect(jsonPath("$.fees[0].fee.id").value(fee.getId()))
				.andExpect(jsonPath("$.fees[0].fee.code").value("FEE00001"))
				.andExpect(jsonPath("$.fees[0].fee.type").value("FLAT"))
				.andExpect(jsonPath("$.fees[0].amount").value(7.5))
				.andExpect(jsonPath("$.fees[0].reasons[0].id").value(reason.getId()))
				.andExpect(jsonPath("$.fees[0].reasons[0].code").value("ELIG0001"))
				.andExpect(jsonPath("$.productId").doesNotExist())
				.andExpect(jsonPath("$.regionId").doesNotExist())
				.andExpect(jsonPath("$.fees[0].feeId").doesNotExist())
				.andExpect(jsonPath("$.formOptions").doesNotExist());
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

	private String pricingPlanRequestJson(String planCode, Long productId, Long regionId, Long feeId) {
		return """
				{
				  "planCode": "%s",
				  "planName": "Test Plan",
				  "productId": %d,
				  "regionId": %d,
				  "activeFrom": "2026-01-01",
				  "activeThrough": "2026-12-31",
				  "fees": [
				    {"feeId": %d, "amount": 7.50, "reasons": []}
				  ],
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(planCode, productId, regionId, feeId);
	}
}
