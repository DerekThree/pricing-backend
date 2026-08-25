package com.pricing.backend.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.branch.BranchEntity;
import com.pricing.backend.branch.BranchRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonConditionEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.fee.FeeEntity;
import com.pricing.backend.fee.FeeRepository;
import com.pricing.backend.generated.model.AttributeType;
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
	private AccountAttributeRepository accountAttributeRepository;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

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
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
		feeRepository.deleteAll();
		regionRepository.deleteAll();
		branchRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void pricesOneFlatFeeEndToEnd() throws Exception {
		saveFeeConfiguration(FeeType.FLAT, new BigDecimal("7.5000"), "STANDARD");

		mockMvc.perform(post("/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(batchRequestJson("PREMIER", null)))
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
	void waivesOneFlatFeeEndToEnd() throws Exception {
		saveFeeConfiguration(FeeType.FLAT, new BigDecimal("7.5000"), "PREMIER");

		mockMvc.perform(post("/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(batchRequestJson(" premier ", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.batchId").value("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b"))
				.andExpect(jsonPath("$.accounts[0].accountNumber").value("ACCOUNT001"))
				.andExpect(jsonPath("$.accounts[0].status").value("OK"))
				.andExpect(jsonPath("$.accounts[0].pricingPlanCode").value("PLAN0001"))
				.andExpect(jsonPath("$.accounts[0].fees[0].feeRequestId").value(1))
				.andExpect(jsonPath("$.accounts[0].fees[0].status").value("OK"))
				.andExpect(jsonPath("$.accounts[0].fees[0].decision").value("WAIVED"))
				.andExpect(jsonPath("$.accounts[0].fees[0].amount").doesNotExist())
				.andExpect(jsonPath("$.accounts[0].fees[0].reasons[0]").value("ELIG0001"));
	}

	@Test
	void pricesOnePercentageFeeEndToEnd() throws Exception {
		saveFeeConfiguration(FeeType.PERCENT, new BigDecimal("5.0000"), "STANDARD");

		mockMvc.perform(post("/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(batchRequestJson("PREMIER", new BigDecimal("123.45"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accounts[0].fees[0].status").value("OK"))
				.andExpect(jsonPath("$.accounts[0].fees[0].decision").value("CHARGED"))
				.andExpect(jsonPath("$.accounts[0].fees[0].amount").value(6.17))
				.andExpect(jsonPath("$.accounts[0].fees[0].reasons").doesNotExist());
	}

	@Test
	void waivesOnePercentageFeeEndToEnd() throws Exception {
		saveFeeConfiguration(FeeType.PERCENT, new BigDecimal("5.0000"), "PREMIER");

		mockMvc.perform(post("/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(batchRequestJson(" premier ", new BigDecimal("10.00"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accounts[0].fees[0].status").value("OK"))
				.andExpect(jsonPath("$.accounts[0].fees[0].decision").value("WAIVED"))
				.andExpect(jsonPath("$.accounts[0].fees[0].amount").doesNotExist())
				.andExpect(jsonPath("$.accounts[0].fees[0].reasons[0]").value("ELIG0001"));
	}

	private void saveFeeConfiguration(
			FeeType feeType, BigDecimal amount, String conditionValue) {
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
				.feeType(feeType)
				.productTypes(List.of(ProductType.DEPOSIT))
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build());
		AccountAttributeEntity attribute = accountAttributeRepository.save(
				AccountAttributeEntity.builder()
						.attributeCode("ATTR0001")
						.attributeName("Account Tier")
						.attributeType(AttributeType.TEXT)
						.productTypes(List.of(ProductType.DEPOSIT))
						.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
						.updatedBy("test")
						.build());
		EligibilityReasonEntity reason = EligibilityReasonEntity.builder()
				.reasonCode("ELIG0001")
				.reasonName("Premier Account")
				.updatedOn(OffsetDateTime.parse("2026-08-01T00:00:00Z"))
				.updatedBy("test")
				.build();
		reason.getConditions().add(EligibilityReasonConditionEntity.builder()
				.reason(reason)
				.attribute(attribute)
				.operator("=")
				.attributeValue(conditionValue)
				.build());
		reason = eligibilityReasonRepository.save(reason);
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
				.amount(amount)
				.reasons(List.of(reason))
				.build());
	}

	private String batchRequestJson(String attributeValue, BigDecimal transactionAmount) {
		String transactionAmountJson = transactionAmount == null
				? ""
				: ",\n      \"transactionAmount\": " + transactionAmount.toPlainString();
		return """
				{
				  "batchId": "5fd2879b-7f17-4a05-8fbe-7ebce6958f3b",
				  "accounts": [{
				    "accountNumber": "ACCOUNT001",
				    "productCode": "PROD0001",
				    "branchCode": "BRANCH001",
				    "pricingDate": "2026-08-31",
				    "attributes": [{
				      "code": "ATTR0001",
				      "value": "%s"
				    }],
				    "fees": [{
				      "feeRequestId": 1,
				      "code": "FEE00001"%s
				    }]
				  }]
				}
				""".formatted(attributeValue, transactionAmountJson);
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
