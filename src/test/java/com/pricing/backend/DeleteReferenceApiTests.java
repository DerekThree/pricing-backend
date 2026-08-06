package com.pricing.backend;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanEntity;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
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
}
