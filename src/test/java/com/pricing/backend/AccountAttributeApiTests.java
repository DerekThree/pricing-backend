package com.pricing.backend;

import java.time.OffsetDateTime;
import java.util.List;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonConditionEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonConditionId;
import com.pricing.backend.eligibilityreason.EligibilityReasonEntity;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.generated.model.AttributeType;
import com.pricing.backend.generated.model.ProductType;
import com.pricing.backend.pricingplan.PricingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountAttributeApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountAttributeRepository accountAttributeRepository;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

	@Autowired
	private PricingPlanRepository pricingPlanRepository;

	@BeforeEach
	void setUp() {
		pricingPlanRepository.deleteAll();
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
	}

	@Test
	void createsAccountAttributeAndListsIt() throws Exception {
		mockMvc.perform(post("/account-attributes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "attributeCode": "ATTR0001",
								  "attributeName": "Account Age",
								  "attributeType": "INTEGER",
								  "productTypes": ["DEPOSIT", "CREDIT"],
								  "updatedBy": "Derek Ochal"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attributeCode").value("ATTR0001"))
				.andExpect(jsonPath("$.attributeName").value("Account Age"))
				.andExpect(jsonPath("$.attributeType").value("INTEGER"))
				.andExpect(jsonPath("$.productTypes[0]").value("DEPOSIT"))
				.andExpect(jsonPath("$.productTypes[1]").value("CREDIT"));

		mockMvc.perform(get("/account-attributes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].attribute").value("ATTR0001 - Account Age"))
				.andExpect(jsonPath("$[0].type").value("INTEGER"))
				.andExpect(jsonPath("$[0].productTypes[0]").value("DEPOSIT"))
				.andExpect(jsonPath("$[0].productTypes[1]").value("CREDIT"));
	}

	@Test
	void rejectsEmptyAndDuplicateProductTypes() throws Exception {
		mockMvc.perform(post("/account-attributes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Applicable Product Types must contain at least one item"));
		mockMvc.perform(post("/account-attributes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("[\"DEPOSIT\", \"DEPOSIT\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("An account attribute cannot contain the same product type twice"));
	}

	@Test
	void rejectsMembershipChangesAndAllowsReorderingWhenAnEligibilityReasonUsesTheAttribute()
			throws Exception {
		AccountAttributeEntity attribute = accountAttributeRepository.save(
				AccountAttributeEntity.builder()
						.attributeCode("ATTR0001")
						.attributeName("Account Age")
						.attributeType(AttributeType.INTEGER)
						.productTypes(List.of(ProductType.DEPOSIT, ProductType.CREDIT))
						.updatedBy("Derek Ochal")
						.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
						.build());
		EligibilityReasonEntity reason = eligibilityReasonRepository.save(
				EligibilityReasonEntity.builder()
						.reasonCode("ELIG0001")
						.reasonName("Account Age")
						.updatedBy("Derek Ochal")
						.updatedOn(OffsetDateTime.parse("2026-06-06T09:00:00+08:00"))
						.build());
		reason.getConditions().add(EligibilityReasonConditionEntity.builder()
				.id(new EligibilityReasonConditionId(reason.getId(), attribute.getId(), ">="))
				.reason(reason)
				.attribute(attribute)
				.attributeValue("18")
				.build());
		eligibilityReasonRepository.saveAndFlush(reason);

		mockMvc.perform(put("/account-attributes/{id}", attribute.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("[\"DEPOSIT\"]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"This account attribute is used by eligibility reason with code ELIG0001. "
								+ "Please update the eligibility reason first."));
		mockMvc.perform(put("/account-attributes/{id}", attribute.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(attributeRequestJson("[\"CREDIT\", \"DEPOSIT\"]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productTypes[0]").value("CREDIT"))
				.andExpect(jsonPath("$.productTypes[1]").value("DEPOSIT"));
	}

	private String attributeRequestJson(String productTypes) {
		return """
				{
				  "attributeCode": "ATTR0001",
				  "attributeName": "Account Age",
				  "attributeType": "INTEGER",
				  "productTypes": %s,
				  "updatedBy": "Derek Ochal"
				}
				""".formatted(productTypes);
	}
}
