package com.pricing.backend;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.FeeType;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeleteReferenceApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BranchRepository branchRepository;

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

	@BeforeEach
	void setUp() {
		simulatorDateService.setCurrentDate(LocalDate.of(2026, 8, 11));
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		feeRepository.deleteAll();
		accountAttributeRepository.deleteAll();
		regionRepository.deleteAll();
		productRepository.deleteAll();
		branchRepository.deleteAll();
	}

	@Test
	void deletingReferencedProductReturnsConflictWithPricingPlanCode() throws Exception {
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
		pricingPlanRepository.save(PricingPlanEntity.builder()
				.planCode("11111111")
				.planName("Premier Midwest")
				.product(product)
				.region(region)
				.activeFrom(LocalDate.parse("2026-01-01"))
				.activeThrough(LocalDate.parse("2026-12-31"))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());

		mockMvc.perform(delete("/products/{id}", product.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This product is used by pricing plan with code 11111111. Please update the pricing plan first."));
	}

	@Test
	void deletingReferencedRegionReturnsConflictWithPricingPlanCode() throws Exception {
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
		pricingPlanRepository.save(PricingPlanEntity.builder()
				.planCode("11111111")
				.planName("Premier Midwest")
				.product(product)
				.region(region)
				.activeFrom(LocalDate.parse("2026-01-01"))
				.activeThrough(LocalDate.parse("2026-12-31"))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());

		mockMvc.perform(delete("/regions/{id}", region.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This region is used by pricing plan with code 11111111. Please update the pricing plan first."));
	}

	@Test
	void deletingReferencedBranchReturnsConflictWithRegionCode() throws Exception {
		BranchEntity branch = branchRepository.save(BranchEntity.builder()
				.branchCode("10000001")
				.branchName("Chicago 104th")
				.state("IL")
				.zipCode("60459")
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());
		regionRepository.save(RegionEntity.builder()
				.regionCode("REG00001")
				.regionName("Midwest")
				.states(List.of())
				.zipCodes(List.of())
				.branches(List.of(branch.getId()))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());

		mockMvc.perform(delete("/branches/{id}", branch.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This branch is used by region with code REG00001. Please update the region first."));
	}

	@Test
	void deletingReferencedAccountAttributeReturnsConflictWithReasonCode() throws Exception {
		AccountAttributeEntity attribute = accountAttributeRepository.save(AccountAttributeEntity.builder()
				.attributeCode("ATTR0001")
				.attributeName("Minimum Balance")
				.attributeType(AttributeType.DECIMAL)
				.productTypes(List.of(ProductType.DEPOSIT))
				.updatedBy("Derek Ochal")
				.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
				.build());

		mockMvc.perform(post("/eligibility-reasons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reasonCode": "ELIG0001",
								  "reasonName": "Minimum Balance",
								  "conditions": [{"attributeId": %d, "operator": ">=", "value": 100}],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(attribute.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/account-attributes/{id}", attribute.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This account attribute is used by eligibility reason with code ELIG0001. "
								+ "Please update the eligibility reason first."));
	}

	@Test
	void deletingReferencedEligibilityReasonReturnsConflictWithPricingPlanCode() throws Exception {
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
				.reasonName("Minimum Balance")
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
								  "fees": [{"feeId": %d, "amount": 7.50, "reasonIds": [%d]}],
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(product.getId(), region.getId(), fee.getId(), reason.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/eligibility-reasons/{id}", reason.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This eligibility reason is used by pricing plan with code PLAN0001. "
								+ "Please update the pricing plan first."));
	}
}
