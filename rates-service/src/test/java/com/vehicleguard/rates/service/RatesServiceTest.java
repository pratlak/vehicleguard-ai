package com.vehicleguard.rates.service;

import com.vehicleguard.rates.exception.RatesNotFoundException;
import com.vehicleguard.rates.model.dto.RatesResponse;
import com.vehicleguard.rates.model.entity.VehicleBaseRate;
import com.vehicleguard.rates.repository.VehicleBaseRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatesServiceTest {

    @Mock
    private VehicleBaseRateRepository repository;

    @InjectMocks
    private RatesService ratesService;

    private VehicleBaseRate sedanRate;
    private VehicleBaseRate sportsRate;

    @BeforeEach
    void setUp() {
        sedanRate = VehicleBaseRate.builder()
                .id(1)
                .vehicleCategory("sedan")
                .baseAnnualPremium(new BigDecimal("1100.00"))
                .description("Standard sedan")
                .build();

        sportsRate = VehicleBaseRate.builder()
                .id(4)
                .vehicleCategory("sports")
                .baseAnnualPremium(new BigDecimal("1800.00"))
                .description("Sports car")
                .build();
    }

    @Test
    void getRates_sedanWithState_returnsCorrectRate() {
        when(repository.findByVehicleCategory("sedan")).thenReturn(Optional.of(sedanRate));

        RatesResponse result = ratesService.getRates("sedan", "CA");

        assertThat(result.getBaseAnnualPremium()).isEqualByComparingTo("1100.00");
        assertThat(result.getVehicleCategory()).isEqualTo("sedan");
        assertThat(result.getState()).isEqualTo("CA");
    }

    @Test
    void getRates_sportsWithoutState_returnsCorrectRate() {
        when(repository.findByVehicleCategory("sports")).thenReturn(Optional.of(sportsRate));

        RatesResponse result = ratesService.getRates("sports", null);

        assertThat(result.getBaseAnnualPremium()).isEqualByComparingTo("1800.00");
        assertThat(result.getVehicleCategory()).isEqualTo("sports");
        assertThat(result.getState()).isNull();
    }

    @Test
    void getRates_normalizesCategory_lowercases() {
        when(repository.findByVehicleCategory("sedan")).thenReturn(Optional.of(sedanRate));

        RatesResponse result = ratesService.getRates("SEDAN", "TX");

        assertThat(result.getVehicleCategory()).isEqualTo("sedan");
        verify(repository).findByVehicleCategory("sedan");
    }

    @Test
    void getRates_unknownCategory_throwsRatesNotFoundException() {
        when(repository.findByVehicleCategory("hovercraft")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratesService.getRates("hovercraft", "CA"))
                .isInstanceOf(RatesNotFoundException.class)
                .hasMessageContaining("hovercraft");
    }

    @Test
    void getRates_stateDoesNotAffectBaseRate() {
        when(repository.findByVehicleCategory("sedan")).thenReturn(Optional.of(sedanRate));

        RatesResponse ca = ratesService.getRates("sedan", "CA");
        RatesResponse ny = ratesService.getRates("sedan", "NY");

        assertThat(ca.getBaseAnnualPremium()).isEqualByComparingTo(ny.getBaseAnnualPremium());
    }
}
