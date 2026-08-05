package com.pricing.backend;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;

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

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		regionRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void createsPricingPlanWithEmbeddedProductAndRegion() throws Exception {
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
				.states(new LinkedHashSet<>())
				.zipCodes(new LinkedHashSet<>())
				.branches(new LinkedHashSet<>())
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
								  "updatedBy": "Derek Ochal"
								}
								""".formatted(product.getId(), region.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.planCode").value("PLAN0001"))
				.andExpect(jsonPath("$.product.id").value(product.getId()))
				.andExpect(jsonPath("$.product.code").value("PROD0001"))
				.andExpect(jsonPath("$.product.type").value("DEPOSIT"))
				.andExpect(jsonPath("$.region.id").value(region.getId()))
				.andExpect(jsonPath("$.region.code").value("REG00001"))
				.andExpect(jsonPath("$.productId").doesNotExist())
				.andExpect(jsonPath("$.regionId").doesNotExist())
				.andExpect(jsonPath("$.formOptions").doesNotExist());
	}
}
