package com.vehicleguard.rates.controller;

import com.vehicleguard.rates.exception.RatesNotFoundException;
import com.vehicleguard.rates.model.dto.RatesResponse;
import com.vehicleguard.rates.service.RatesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatesController.class)
class RatesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RatesService ratesService;

    @Test
    void getRates_validCategory_returns200() throws Exception {
        RatesResponse response = RatesResponse.builder()
                .baseAnnualPremium(new BigDecimal("1100.00"))
                .vehicleCategory("sedan")
                .state("CA")
                .build();

        when(ratesService.getRates("sedan", "CA")).thenReturn(response);

        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "sedan")
                        .param("state", "CA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.baseAnnualPremium", is(1100.00)))
                .andExpect(jsonPath("$.vehicleCategory", is("sedan")))
                .andExpect(jsonPath("$.state", is("CA")));
    }

    @Test
    void getRates_sportsCategory_returns1800() throws Exception {
        RatesResponse response = RatesResponse.builder()
                .baseAnnualPremium(new BigDecimal("1800.00"))
                .vehicleCategory("sports")
                .state("TX")
                .build();

        when(ratesService.getRates("sports", "TX")).thenReturn(response);

        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "sports")
                        .param("state", "TX")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAnnualPremium", is(1800.00)));
    }

    @Test
    void getRates_unknownCategory_returns404() throws Exception {
        when(ratesService.getRates(anyString(), anyString()))
                .thenThrow(new RatesNotFoundException("No base rate found for vehicle category: hovercraft"));

        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "hovercraft")
                        .param("state", "CA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void getRates_missingCategory_returns400() throws Exception {
        mockMvc.perform(get("/rates")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRates_withoutState_returns200() throws Exception {
        RatesResponse response = RatesResponse.builder()
                .baseAnnualPremium(new BigDecimal("1300.00"))
                .vehicleCategory("suv")
                .state(null)
                .build();

        when(ratesService.getRates("suv", null)).thenReturn(response);

        mockMvc.perform(get("/rates")
                        .param("vehicleCategory", "suv")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAnnualPremium", is(1300.00)));
    }
}
