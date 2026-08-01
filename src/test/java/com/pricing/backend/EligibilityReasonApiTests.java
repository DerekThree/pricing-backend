package com.pricing.backend;

import java.time.OffsetDateTime;

import com.pricing.backend.accountattribute.AccountAttributeEntity;
import com.pricing.backend.accountattribute.AccountAttributeRepository;
import com.pricing.backend.eligibilityreason.EligibilityReasonRepository;
import com.pricing.backend.generated.model.AccountAttributeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EligibilityReasonApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EligibilityReasonRepository eligibilityReasonRepository;

	@Autowired
	private AccountAttributeRepository accountAttributeRepository;

	@BeforeEach
	void setUp() {
		eligibilityReasonRepository.deleteAll();
		accountAttributeRepository.deleteAll();
	}

	@Test
	void createsEligibilityReasonFromJsonAndListsIt() throws Exception {
		AccountAttributeEntity amount = saveAttribute("ATTR0001", "Min Amount", AccountAttributeType.DECIMAL);
		AccountAttributeEntity active = saveAttribute("ATTR0002", "Active", AccountAttributeType.BOOLEAN);

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
				.andExpect(jsonPath("$.formOptions.attributes", hasSize(2)))
				.andExpect(jsonPath("$.formOptions.attributes[0].id").value(amount.getId()))
				.andExpect(jsonPath("$.formOptions.attributes[0].type").value("DECIMAL"))
				.andExpect(jsonPath("$.formOptions.attributes[1].id").value(active.getId()))
				.andExpect(jsonPath("$.formOptions.attributes[1].type").value("BOOLEAN"));

		mockMvc.perform(get("/eligibility-reasons"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].eligibilityReason").value("ELIG0001 - Min. Balance"))
				.andExpect(jsonPath("$[0].conditions", contains("ATTR0001 >= 100.5", "ATTR0002 = true")));
	}

	@Test
	void returnsEligibilityReasonOptions() throws Exception {
		saveAttribute("ATTR0001", "Min Amount", AccountAttributeType.INTEGER);
		saveAttribute("ATTR0002", "Active", AccountAttributeType.BOOLEAN);

		mockMvc.perform(get("/eligibility-reasons/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attributes", hasSize(2)))
				.andExpect(jsonPath("$.attributes[0].code").value("ATTR0001"))
				.andExpect(jsonPath("$.attributes[0].type").value("INTEGER"))
				.andExpect(jsonPath("$.attributes[1].code").value("ATTR0002"))
				.andExpect(jsonPath("$.attributes[1].type").value("BOOLEAN"));
	}

	private AccountAttributeEntity saveAttribute(String code, String name, AccountAttributeType type) {
		return accountAttributeRepository.save(new AccountAttributeEntity(
				null,
				code,
				name,
				type,
				OffsetDateTime.parse("2026-06-06T09:00:00+08:00"),
				"Derek Ochal"
		));
	}
}
