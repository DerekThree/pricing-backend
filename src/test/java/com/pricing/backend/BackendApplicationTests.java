package com.pricing.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void publishesOpenApiContract() throws Exception {
		mockMvc.perform(get("/openapi.yaml"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("title: Pricing Backend API")));
	}

	@Test
	void exposesSwaggerUi() throws Exception {
		mockMvc.perform(get("/docs"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/swagger-ui/index.html"));
	}

	@Test
	void servesApiImplementedFromGeneratedInterface() throws Exception {
		mockMvc.perform(get("/branches"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].branchCode").value("10000001"));
	}

}
