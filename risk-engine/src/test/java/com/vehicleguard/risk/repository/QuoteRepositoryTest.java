package com.vehicleguard.risk.repository;

import com.vehicleguard.risk.integration.AbstractIntegrationTest;
import com.vehicleguard.risk.model.entity.QuoteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class QuoteRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteRepository quoteRepository;

    @Test
    void save_validQuote_persistsAndReturnsWithId() {
        QuoteRequest quote = QuoteRequest.builder()
                .createdAt(LocalDateTime.now())
                .driverAge(28)
                .licenseYears(6)
                .violationsLast5yr(0)
                .accidentsLast5yr(0)
                .vehicleMake("Toyota")
                .vehicleModel("Camry")
                .vehicleYear(2020)
                .vehicleCategory("sedan")
                .zipCode("90210")
                .stateCode("CA")
                .coverageType("FULL")
                .riskScore(BigDecimal.valueOf(15.5))
                .riskTier("LOW")
                .annualPremiumUsd(BigDecimal.valueOf(1650.00))
                .monthlyPremiumUsd(BigDecimal.valueOf(137.50))
                .appliedFactorsJson("[]")
                .aiExplanation("Low risk profile.")
                .inputPayloadJson("{}")
                .build();

        QuoteRequest saved = quoteRepository.save(quote);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDriverAge()).isEqualTo(28);
    }

    @Test
    void findById_existingQuote_returnsQuote() {
        QuoteRequest quote = QuoteRequest.builder()
                .createdAt(LocalDateTime.now())
                .driverAge(35)
                .licenseYears(12)
                .violationsLast5yr(0)
                .accidentsLast5yr(0)
                .vehicleCategory("sedan")
                .zipCode("55555")
                .coverageType("LIABILITY")
                .riskScore(BigDecimal.ZERO)
                .riskTier("LOW")
                .annualPremiumUsd(BigDecimal.valueOf(1100.00))
                .monthlyPremiumUsd(BigDecimal.valueOf(91.67))
                .appliedFactorsJson("[]")
                .aiExplanation("Clean record.")
                .inputPayloadJson("{}")
                .build();

        QuoteRequest saved = quoteRepository.save(quote);
        Optional<QuoteRequest> found = quoteRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDriverAge()).isEqualTo(35);
        assertThat(found.get().getRiskTier()).isEqualTo("LOW");
    }

    @Test
    void findById_nonExistentId_returnsEmpty() {
        Optional<QuoteRequest> result = quoteRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}
