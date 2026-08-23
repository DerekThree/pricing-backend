package com.pricing.backend.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.FeeType;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanEntity;
import com.pricing.backend.pricingplan.PricingPlanFeeEntity;
import com.pricing.backend.pricingplan.PricingPlanFeeId;
import com.pricing.backend.pricingplan.PricingPlanFeeRepository;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import com.pricing.backend.product.ProductEntity;
import com.pricing.backend.product.ProductRepository;
import com.pricing.backend.region.RegionEntity;
import com.pricing.backend.region.RegionRepository;
import org.junit.jupiter.api.AfterEach;
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
class BatchApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private FeeRepository feeRepository;

	@Autowired
	private PricingPlanFeeRepository pricingPlanFeeRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private RegionRepository regionRepository;

	@BeforeEach
	void setUp() {
		cleanUp();
	}

	@AfterEach
	void cleanUp() {
		pricingPlanRepository.deleteAll();
		feeRepository.deleteAll();
		regionRepository.deleteAll();
		branchRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void pricesOneFlatFeeEndToEnd() throws Exception {
		ProductEntity product = productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build());
		BranchEntity branch = branchRepository.save(BranchEntity.builder()
				.branchCode("BRANCH001")
				.branchName("Chicago 104th St.")
				.state("IL")
				.zipCode("60459")
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build());
		RegionEntity region = regionRepository.save(RegionEntity.builder()
				.regionCode("REGION001")
				.regionName("Midwest")
				.branches(List.of(branch.getId()))
				.states(List.of())
				.zipCodes(List.of())
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build());
		FeeEntity fee = feeRepository.save(FeeEntity.builder()
				.feeCode("FEE00001")
				.feeName("Monthly Maintenance Fee")
				.feeType(FeeType.FLAT)
				.productTypes(List.of(ProductType.DEPOSIT))
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build());
		PricingPlanEntity pricingPlan = pricingPlanRepository.save(PricingPlanEntity.builder()
				.planCode("PLAN0001")
				.planName("Premier Midwest")
				.product(product)
				.region(region)
				.activeFrom(LocalDate.of(2026, 8, 1))
				.activeThrough(LocalDate.of(2026, 8, 31))
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build());
		pricingPlanFeeRepository.save(PricingPlanFeeEntity.builder()
				.id(new PricingPlanFeeId(pricingPlan.getId(), fee.getId()))
				.pricingPlan(pricingPlan)
				.fee(fee)
				.amount(new BigDecimal("7.5000"))
				.build());

		mockMvc.perform(post("/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "batchId": "5fd2879b-7f17-4a05-8fbe-7ebce6958f3b",
								  "accounts": [{
								    "accountNumber": "ACCOUNT001",
								    "productCode": "PROD0001",
								    "branchCode": "BRANCH001",
								    "pricingDate": "2026-08-31",
								    "attributes": [{
								      "code": "ATTR0001",
								      "value": "PREMIER"
								    }],
								    "fees": [{
								      "feeRequestId": 1,
								      "code": "FEE00001"
								    }]
								  }]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.batchId").value("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b"))
				.andExpect(jsonPath("$.accounts[0].accountNumber").value("ACCOUNT001"))
				.andExpect(jsonPath("$.accounts[0].status").value("OK"))
				.andExpect(jsonPath("$.accounts[0].pricingPlanCode").value("PLAN0001"))
				.andExpect(jsonPath("$.accounts[0].fees[0].feeRequestId").value(1))
				.andExpect(jsonPath("$.accounts[0].fees[0].status").value("OK"))
				.andExpect(jsonPath("$.accounts[0].fees[0].decision").value("CHARGED"))
				.andExpect(jsonPath("$.accounts[0].fees[0].amount").value(7.50))
				.andExpect(jsonPath("$.accounts[0].fees[0].reasons").doesNotExist());
	}

	@Test
	void returnsFailedAccountResolutionWithoutConfigurationDetails() throws Exception {
		productRepository.save(ProductEntity.builder()
				.productCode("PROD0001")
				.productName("Premier Checking")
				.productType(ProductType.DEPOSIT)
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build());

		mockMvc.perform(post("/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "batchId": "5fd2879b-7f17-4a05-8fbe-7ebce6958f3b",
								  "accounts": [{
								    "accountNumber": "ACCOUNT001",
								    "productCode": "PROD0001",
								    "branchCode": "BRANCH001",
								    "pricingDate": "2026-08-31",
								    "attributes": [],
								    "fees": [{
								      "feeRequestId": 1,
								      "code": "FEE00001"
								    }]
								  }]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accounts[0].accountNumber").value("ACCOUNT001"))
				.andExpect(jsonPath("$.accounts[0].status").value("BRANCH_NOT_FOUND"))
				.andExpect(jsonPath("$.accounts[0].pricingPlanCode").doesNotExist())
				.andExpect(jsonPath("$.accounts[0].fees").doesNotExist());
	}

	@Test
	void rejectsOverlongAccountNumber() throws Exception {
		mockMvc.perform(post("/batch")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "batchId": "5fd2879b-7f17-4a05-8fbe-7ebce6958f3b",
							  "accounts": [{
							    "accountNumber": "ACCOUNTNUMBER12345678901234",
							    "productCode": "PROD0001",
							    "branchCode": "BRANCH001",
							    "pricingDate": "2026-08-31",
							    "attributes": [],
							    "fees": [{
							      "feeRequestId": 1,
							      "code": "FEE00001"
							    }]
							  }]
							}
							"""))
				.andExpect(status().isBadRequest());
	}

}
