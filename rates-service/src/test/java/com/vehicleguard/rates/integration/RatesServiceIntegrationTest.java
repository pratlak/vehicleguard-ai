package com.vehicleguard.rates.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class RatesServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRates_sports_returns1800() throws Exception {
        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "sports")
                        .param("state", "CA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.baseAnnualPremium", is(1800.00)))
                .andExpect(jsonPath("$.vehicleCategory", is("sports")))
                .andExpect(jsonPath("$.state", is("CA")));
    }

    @Test
    void getRates_sedan_returns1100() throws Exception {
        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "sedan")
                        .param("state", "WA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAnnualPremium", is(1100.00)))
                .andExpect(jsonPath("$.vehicleCategory", is("sedan")))
                .andExpect(jsonPath("$.state", is("WA")));
    }

    @Test
    void getRates_suv_returns1300() throws Exception {
        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "suv")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAnnualPremium", is(1300.00)));
    }

    @Test
    void getRates_luxury_returns2200() throws Exception {
        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "luxury")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAnnualPremium", is(2200.00)));
    }

    @Test
    void getRates_unknownCategory_returns404() throws Exception {
        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "hovercraft")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void getRates_missingCategory_returns400() throws Exception {
        mockMvc.perform(get("/rates").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actuatorHealth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
