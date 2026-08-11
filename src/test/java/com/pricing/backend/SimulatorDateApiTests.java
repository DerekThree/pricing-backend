package com.pricing.backend;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulatorDateApiTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Clock clock;

	@BeforeEach
	void setUp() {
		when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		when(clock.instant()).thenReturn(Instant.parse("2026-08-11T00:00:00Z"));
	}

	@Test
	void readsConfiguredSystemDate() throws Exception {
		mockMvc.perform(get("/simulator/date"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentDate").value("2026-08-11"));
	}

	@Test
	void setsAndRepeatedlyChangesApplicationDate() throws Exception {
		setDate("2026-09-01");
		readDate("2026-09-01");
		setDate("2026-10-15");
		readDate("2026-10-15");
	}

	private void setDate(String date) throws Exception {
		mockMvc.perform(put("/simulator/date")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"currentDate\":\"%s\"}".formatted(date)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentDate").value(date));
	}

	private void readDate(String date) throws Exception {
		mockMvc.perform(get("/simulator/date"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentDate").value(date));
	}
}
