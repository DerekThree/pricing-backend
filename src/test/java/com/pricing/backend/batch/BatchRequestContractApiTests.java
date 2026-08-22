package com.pricing.backend.batch;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.pricing.engine.AccountBatch;
import com.pricing.engine.AccountBatchResult;
import com.pricing.engine.AccountBatchResult.AccountResult;
import com.pricing.engine.AccountBatchResult.AccountStatus;
import com.pricing.engine.AccountBatchResult.FeeResult;
import com.pricing.engine.AccountBatchResult.FeeStatus;
import com.pricing.engine.RuleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.pricing.engine.AccountBatchResult.AccountStatus.OK;
import static com.pricing.engine.AccountBatchResult.Decision.CHARGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BatchRequestContractApiTests {

	private static final String BATCH_ID = "5fd2879b-7f17-4a05-8fbe-7ebce6958f3b";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RuleEngine ruleEngine;

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	void rejectsRequestContractViolationsBeforePricing(
			String scenario,
			String request) throws Exception {
		mockMvc.perform(post("/batch")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(ruleEngine);
	}

	@Test
	void mapsEveryValidAccountAttributeScalar() throws Exception {
		UUID batchId = UUID.fromString(BATCH_ID);
		AccountBatchResult result = new AccountBatchResult(batchId, List.of(new AccountResult(
				"ACCOUNT001",
				AccountStatus.ERROR,
				null,
				null)));
		when(ruleEngine.price(any())).thenReturn(result);

		mockMvc.perform(post("/batch")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestWithAccount(validAccount().replace(
							"\"attributes\": []",
							"\"attributes\": ["
									+ "{\"code\":\"TEXT\",\"value\":\"Value\"},"
									+ "{\"code\":\"NUMBER\",\"value\":12.50},"
									+ "{\"code\":\"BOOLEAN\",\"value\":true}]"))))
				.andExpect(status().isOk());

		ArgumentCaptor<AccountBatch> batchCaptor = ArgumentCaptor.forClass(AccountBatch.class);
		verify(ruleEngine).price(batchCaptor.capture());
		List<AccountBatch.AccountAttribute> attributes = batchCaptor.getValue()
				.accounts().getFirst().attributes();
		assertEquals("Value", attributes.get(0).value());
		assertEquals(new BigDecimal("12.50"), attributes.get(1).value());
		assertEquals(true, attributes.get(2).value());
	}

	@Test
	void allowsFeeRequestIdsAcrossAccountsAndRepeatedFeeCodesWithinAnAccount() throws Exception {
		UUID batchId = UUID.fromString(BATCH_ID);
		when(ruleEngine.price(any())).thenReturn(new AccountBatchResult(batchId, List.of(
				new AccountResult("ACCOUNT001", AccountStatus.ERROR, null, null),
				new AccountResult("ACCOUNT002", AccountStatus.ERROR, null, null))));
		String firstAccount = validAccount()
				.replace(validFee(), validFee()
						+ ","
						+ validFee().replace("\"feeRequestId\": 1", "\"feeRequestId\": 2"));
		String secondAccount = validAccount()
				.replace("\"accountNumber\": \"ACCOUNT001\"", "\"accountNumber\": \"ACCOUNT002\"");

		mockMvc.perform(post("/batch")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestWithAccounts("[" + firstAccount + "," + secondAccount + "]")))
				.andExpect(status().isOk());

		ArgumentCaptor<AccountBatch> batchCaptor = ArgumentCaptor.forClass(AccountBatch.class);
		verify(ruleEngine).price(batchCaptor.capture());
		List<AccountBatch.Account> accounts = batchCaptor.getValue().accounts();
		assertEquals(List.of(1L, 2L), accounts.get(0).fees().stream()
				.map(AccountBatch.FeeRequest::feeRequestId)
				.toList());
		assertEquals(1L, accounts.get(1).fees().getFirst().feeRequestId());
		assertEquals(List.of("FEE00001", "FEE00001"), accounts.get(0).fees().stream()
				.map(AccountBatch.FeeRequest::code)
				.toList());
	}

	@Test
	void preservesValidCorrelationValuesAndTransactionAmount() throws Exception {
		UUID batchId = UUID.fromString("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b");
		AccountBatchResult result = new AccountBatchResult(batchId, List.of(new AccountResult(
				"ACCOUNT01",
				OK,
				"PLAN0001",
				List.of(new FeeResult(
						123L,
						FeeStatus.OK,
						CHARGED,
						new BigDecimal("1.00"),
						null)))));
		when(ruleEngine.price(any())).thenReturn(result);

		mockMvc.perform(post("/batch")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "batchId": "5fd2879b-7f17-4a05-8fbe-7ebce6958f3b",
							  "accounts": [{
							    "accountNumber": "ACCOUNT01",
							    "productCode": "PROD0001",
							    "branchCode": "BRANCH001",
							    "pricingDate": "2026-08-31",
							    "attributes": [],
							    "fees": [{
							      "feeRequestId": 123,
							      "code": "FEE00001",
							      "transactionAmount": 123.45
							    }]
							  }]
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.batchId").value("5fd2879b-7f17-4a05-8fbe-7ebce6958f3b"))
				.andExpect(jsonPath("$.accounts[0].accountNumber").value("ACCOUNT01"))
				.andExpect(jsonPath("$.accounts[0].fees[0].feeRequestId").value(123));

		ArgumentCaptor<AccountBatch> batchCaptor = ArgumentCaptor.forClass(AccountBatch.class);
		verify(ruleEngine).price(batchCaptor.capture());
		AccountBatch batch = batchCaptor.getValue();
		assertEquals(batchId, batch.batchId());
		assertEquals("ACCOUNT01", batch.accounts().getFirst().accountNumber());
		assertEquals(123L, batch.accounts().getFirst().fees().getFirst().feeRequestId());
		assertEquals(new BigDecimal("123.45"),
				batch.accounts().getFirst().fees().getFirst().transactionAmount());
	}

	@Test
	void rejectsDuplicateAccountNumbersBeforePricing() throws Exception {
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
							  }, {
							    "accountNumber": "ACCOUNT001",
							    "productCode": "PROD0002",
							    "branchCode": "BRANCH002",
							    "pricingDate": "2026-09-01",
							    "attributes": [],
							    "fees": [{
							      "feeRequestId": 2,
							      "code": "FEE00002"
							    }]
							  }]
							}
							"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(ruleEngine);
	}

	@Test
	void rejectsDuplicateFeeRequestIdsWithinAnAccountBeforePricing() throws Exception {
		String account = validAccount().replace(validFee(), validFee() + "," + validFee());

		mockMvc.perform(post("/batch")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestWithAccount(account)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(ruleEngine);
	}

	private static Stream<Arguments> invalidRequests() {
		String account = validAccount();
		String attribute = "{\"code\":\"ATTR0001\",\"value\":\"Value\"}";

		return Stream.of(
				arguments("malformed JSON", "{"),
				arguments("missing batch ID", "{\"accounts\":[" + account + "]}"),
				arguments("invalid batch ID", validRequest().replace(BATCH_ID, "not-a-uuid")),
				arguments("missing accounts", "{\"batchId\":\"" + BATCH_ID + "\"}"),
				arguments("null accounts", requestWithAccounts("null")),
				arguments("empty accounts", requestWithAccounts("[]")),
				arguments("null account", requestWithAccounts("[null]")),
				arguments("missing account number", requestWithAccount(account.replace(
						"\"accountNumber\": \"ACCOUNT001\",", ""))),
				arguments("missing Product code", requestWithAccount(account.replace(
						"\"productCode\": \"PROD0001\",", ""))),
				arguments("missing Branch code", requestWithAccount(account.replace(
						"\"branchCode\": \"BRANCH001\",", ""))),
				arguments("missing Pricing Date", requestWithAccount(account.replace(
						"\"pricingDate\": \"2026-08-31\",", ""))),
				arguments("missing Attributes", requestWithAccount(account.replace(
						"\"attributes\": [],", ""))),
				arguments("null Attributes", requestWithAccount(account.replace(
						"\"attributes\": []", "\"attributes\": null"))),
				arguments("null Attribute", requestWithAccount(account.replace(
						"\"attributes\": []", "\"attributes\": [null]"))),
				arguments("missing Fees", requestWithAccount(account.replace(
						",\n  \"fees\": [" + validFee() + "]", ""))),
				arguments("null Fees", requestWithAccount(account.replace(
						"\"fees\": [" + validFee() + "]", "\"fees\": null"))),
				arguments("empty Fees", requestWithAccount(account.replace(
						"\"fees\": [" + validFee() + "]", "\"fees\": []"))),
				arguments("null Fee", requestWithAccount(account.replace(
						"\"fees\": [" + validFee() + "]", "\"fees\": [null]"))),
				arguments("empty account number", requestWithAccount(account.replace("ACCOUNT001", ""))),
				arguments("blank account number", requestWithAccount(account.replace("ACCOUNT001", "   "))),
				arguments("lowercase account number", requestWithAccount(account.replace(
						"ACCOUNT001", "account001"))),
				arguments("punctuated account number", requestWithAccount(account.replace(
						"ACCOUNT001", "ACCOUNT-01"))),
				arguments("overlong account number", requestWithAccount(account.replace(
						"ACCOUNT001", "ACCOUNTNUMBER12345678901234"))),
				arguments("missing Attribute code", requestWithAttribute(
						"{\"value\":\"Value\"}")),
				arguments("missing Attribute value", requestWithAttribute(
						"{\"code\":\"ATTR0001\"}")),
				arguments("object Attribute value", requestWithAttribute(
						"{\"code\":\"ATTR0001\",\"value\":{}}")),
				arguments("array Attribute value", requestWithAttribute(
						"{\"code\":\"ATTR0001\",\"value\":[]}")),
				arguments("null Attribute value", requestWithAttribute(
						"{\"code\":\"ATTR0001\",\"value\":null}")),
				arguments("missing Fee Request ID", requestWithAccount(account.replace(
						"\"feeRequestId\": 1,", ""))),
				arguments("null Fee Request ID", requestWithAccount(account.replace(
						"\"feeRequestId\": 1", "\"feeRequestId\": null"))),
				arguments("nonnumeric Fee Request ID", requestWithAccount(account.replace(
						"\"feeRequestId\": 1", "\"feeRequestId\": \"not-an-id\""))),
				arguments("missing Fee code", requestWithAccount(account.replace(
						",\n    \"code\": \"FEE00001\"", ""))),
				arguments("lowercase Product code", requestWithAccount(
						account.replace("PROD0001", "prod0001"))),
				arguments("overlong Product code", requestWithAccount(account.replace(
						"PROD0001", "PRODUCTCODE123456789012345"))),
				arguments("lowercase Branch code", requestWithAccount(
						account.replace("BRANCH001", "branch001"))),
				arguments("overlong Branch code", requestWithAccount(account.replace(
						"BRANCH001", "BRANCHCODE1234567890123456"))),
				arguments("lowercase Attribute code", requestWithAttribute(attribute.replace(
						"ATTR0001", "attr0001"))),
				arguments("overlong Attribute code", requestWithAttribute(attribute.replace(
						"ATTR0001", "ATTRIBUTECODE12345678901234"))),
				arguments("lowercase Fee code", requestWithAccount(account.replace("FEE00001", "fee00001"))),
				arguments("overlong Fee code", requestWithAccount(account.replace(
						"FEE00001", "FEECODE1234567890123456789"))),
				arguments("null transaction amount", requestWithTransactionAmount("null")),
				arguments("negative transaction amount", requestWithTransactionAmount("-0.01")),
				arguments("sub-cent transaction amount", requestWithTransactionAmount("0.001"))
		);
	}

	private static String validRequest() {
		return requestWithAccount(validAccount());
	}

	private static String requestWithAttribute(String attribute) {
		return requestWithAccount(validAccount().replace(
				"\"attributes\": []",
				"\"attributes\": [" + attribute + "]"));
	}

	private static String requestWithTransactionAmount(String amount) {
		return requestWithAccount(validAccount().replace(
				"\"code\": \"FEE00001\"",
				"\"code\": \"FEE00001\",\"transactionAmount\":" + amount));
	}

	private static String requestWithAccount(String account) {
		return requestWithAccounts("[" + account + "]");
	}

	private static String requestWithAccounts(String accounts) {
		return "{\"batchId\":\"" + BATCH_ID + "\",\"accounts\":" + accounts + "}";
	}

	private static String validAccount() {
		return """
				{
				  "accountNumber": "ACCOUNT001",
				  "productCode": "PROD0001",
				  "branchCode": "BRANCH001",
				  "pricingDate": "2026-08-31",
				  "attributes": [],
				  "fees": [%s]
				}
				""".formatted(validFee()).strip();
	}

	private static String validFee() {
		return """
				{
				    "feeRequestId": 1,
				    "code": "FEE00001"
				  }
				""".strip();
	}
}
