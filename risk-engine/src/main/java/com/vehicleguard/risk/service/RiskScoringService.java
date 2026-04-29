package com.vehicleguard.risk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.exception.RiskScoringException;
import com.vehicleguard.risk.model.dto.AppliedFactor;
import com.vehicleguard.risk.model.dto.AssessRequest;
import com.vehicleguard.risk.model.dto.AssessResponse;
import com.vehicleguard.risk.model.entity.QuoteRequest;
import com.vehicleguard.risk.model.entity.RiskFactor;
import com.vehicleguard.risk.repository.QuoteRepository;
import com.vehicleguard.risk.repository.RiskFactorRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class RiskScoringService {

    private final RiskFactorRepository riskFactorRepository;
    private final QuoteRepository quoteRepository;
    private final AIExplanationService aiExplanationService;
    private final WebClient ratesWebClient;
    private final ObjectMapper objectMapper;

    private List<RiskFactor> cachedFactors = new ArrayList<>();

    // Maximum achievable score (one person can only trigger one age group, one violation tier, etc.)
    private static final double MAX_POSSIBLE_SCORE = 90.0;

    private static final Set<String> HIGH_DENSITY_ZIPS = Set.of(
            "10001","10002","10003","10004","10005","10006","10007","10008","10009","10010",
            "10011","10012","10013","10014","10016","10017","10018","10019","10020","10021",
            "90001","90002","90003","90004","90005","90006","90007","90008","90009","90010",
            "90011","90012","90013","90014","90015","90016","90017","90018","90019","90020",
            "60601","60602","60603","60604","60605","60606","60607","60608","60609","60610",
            "77001","77002","77003","77004","77005","77006","77007","77008","77009","77010",
            "19101","19102","19103","19104","19105","19106","19107","19108","19109","19110",
            "98001","98002","98003","98004","98005","98006","98007","98008","98101","98102",
            "30301","30302","30303","30304","30305","30306","30307","30308","30309","30310"
    );

    private static final Set<String> SPORTS_MODELS = Set.of(
            "mustang","camaro","corvette","911","m3","wrx","challenger","charger","supra",
            "370z","400z","86","brz","miata","mx-5","viper","gt","gt350","gt500","shelby",
            "nsx","rx-7","rx7","rx-8","rx8","gtr","gt-r","evo","evolution","lancer evolution"
    );

    private static final Set<String> LUXURY_MODELS = Set.of(
            "7-series","s-class","a8","ls","escalade","bentley","rolls-royce","rolls royce",
            "phantom","ghost","wraith","cullinan","continental","flying spur","mulsanne",
            "maybach","s500","s550","s600","s63","s65","750i","760i","a8l","ls500","ls600"
    );

    private static final Set<String> LUXURY_MAKES = Set.of(
            "bentley","rolls-royce","rolls royce","maserati","ferrari","lamborghini","aston martin",
            "bugatti","pagani","koenigsegg","mclaren"
    );

    private static final Set<String> TRUCK_MODELS = Set.of(
            "f-150","f150","silverado","ram","tacoma","tundra","ranger","colorado","frontier",
            "ridgeline","canyon","titan","1500","2500","3500","f-250","f250","f-350","f350",
            "f-450","f450","maverick","santa cruz"
    );

    private static final Set<String> SUV_MODELS = Set.of(
            "explorer","rav4","cr-v","crv","pilot","suburban","tahoe","highlander","pathfinder",
            "4runner","sequoia","expedition","navigator","escalade suv","durango","grand cherokee",
            "cherokee","compass","renegade","wrangler","gladiator","bronco","blazer","equinox",
            "traverse","enclave","envoy","acadia","terrain","santa fe","tucson","palisade",
            "telluride","sorento","sportage","cx-5","cx5","cx-9","cx9","mdx","rdx","qx50",
            "qx60","qx80","x3","x5","x7","q5","q7","q8","glc","gle","gls","gla","glb","glk"
    );

    private static final Set<String> ELECTRIC_MAKES = Set.of("tesla","rivian","polestar","lucid","fisker");
    private static final Set<String> ELECTRIC_MODELS = Set.of(
            "leaf","bolt","ioniq","ioniq5","ioniq6","kona electric","niro ev","ev6","mustang mach-e",
            "mach-e","id.4","id4","bz4x","lyriq","hummer ev"
    );

    public RiskScoringService(RiskFactorRepository riskFactorRepository,
                               QuoteRepository quoteRepository,
                               AIExplanationService aiExplanationService,
                               @Qualifier("ratesWebClient") WebClient ratesWebClient,
                               ObjectMapper objectMapper) {
        this.riskFactorRepository = riskFactorRepository;
        this.quoteRepository = quoteRepository;
        this.aiExplanationService = aiExplanationService;
        this.ratesWebClient = ratesWebClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadRiskFactors() {
        cachedFactors = riskFactorRepository.findByIsActiveTrue();
        log.info("Loaded {} active risk factors", cachedFactors.size());
    }

    @Transactional
    public AssessResponse assess(AssessRequest request) {
        log.info("Assessing risk for driverAge={}, vehicleModel={}, coverageType={}",
                request.getDriverAge(), request.getVehicleModel(), request.getCoverageType());
        try {
            String vehicleCategory = resolveVehicleCategory(request);
            int currentYear = LocalDateTime.now().getYear();

            List<AppliedFactor> appliedFactors = evaluateFactors(request, vehicleCategory, currentYear);

            double rawScore = appliedFactors.stream()
                    .mapToDouble(f -> f.getScoreImpact().doubleValue())
                    .sum();
            double normalizedScore = Math.min((rawScore / MAX_POSSIBLE_SCORE) * 100.0, 100.0);
            BigDecimal riskScore = BigDecimal.valueOf(normalizedScore).setScale(2, RoundingMode.HALF_UP);
            String riskTier = assignRiskTier(normalizedScore);

            BigDecimal baseRate = fetchBaseRate(vehicleCategory, request.getStateCode());
            BigDecimal annualPremium = calculatePremium(baseRate, appliedFactors);
            BigDecimal monthlyPremium = annualPremium.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

            String aiExplanation = aiExplanationService.generateExplanation(request, appliedFactors, riskScore, riskTier);

            String appliedFactorsJson = toJson(appliedFactors);
            String inputPayloadJson = toJson(request);

            QuoteRequest entity = QuoteRequest.builder()
                    .createdAt(LocalDateTime.now())
                    .driverAge(request.getDriverAge())
                    .licenseYears(request.getLicenseYears())
                    .violationsLast5yr(request.getViolationsLast5Yr())
                    .accidentsLast5yr(request.getAccidentsLast5Yr())
                    .vehicleMake(request.getVehicleMake())
                    .vehicleModel(request.getVehicleModel())
                    .vehicleYear(request.getVehicleYear())
                    .vehicleCategory(vehicleCategory)
                    .zipCode(request.getZipCode())
                    .stateCode(request.getStateCode())
                    .coverageType(request.getCoverageType())
                    .riskScore(riskScore)
                    .riskTier(riskTier)
                    .annualPremiumUsd(annualPremium)
                    .monthlyPremiumUsd(monthlyPremium)
                    .appliedFactorsJson(appliedFactorsJson)
                    .aiExplanation(aiExplanation)
                    .inputPayloadJson(inputPayloadJson)
                    .build();

            QuoteRequest saved = quoteRepository.save(entity);
            log.info("Quote saved: id={}, riskTier={}, premium={}", saved.getId(), riskTier, annualPremium);

            return AssessResponse.builder()
                    .quoteId(saved.getId())
                    .riskScore(riskScore)
                    .riskTier(riskTier)
                    .annualPremiumUsd(annualPremium)
                    .monthlyPremiumUsd(monthlyPremium)
                    .coverageType(request.getCoverageType())
                    .appliedFactors(appliedFactors)
                    .aiExplanation(aiExplanation)
                    .createdAt(saved.getCreatedAt())
                    .build();

        } catch (RiskScoringException e) {
            throw e;
        } catch (Exception e) {
            throw new RiskScoringException("Failed to assess risk: " + e.getMessage(), e);
        }
    }

    public AssessResponse getQuote(UUID quoteId) {
        QuoteRequest entity = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new com.vehicleguard.risk.exception.QuoteNotFoundException(
                        "Quote not found: " + quoteId));
        try {
            List<AppliedFactor> appliedFactors = objectMapper.readValue(
                    entity.getAppliedFactorsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AppliedFactor.class));
            return AssessResponse.builder()
                    .quoteId(entity.getId())
                    .riskScore(entity.getRiskScore())
                    .riskTier(entity.getRiskTier())
                    .annualPremiumUsd(entity.getAnnualPremiumUsd())
                    .monthlyPremiumUsd(entity.getMonthlyPremiumUsd())
                    .coverageType(entity.getCoverageType())
                    .appliedFactors(appliedFactors)
                    .aiExplanation(entity.getAiExplanation())
                    .createdAt(entity.getCreatedAt())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RiskScoringException("Failed to parse quote data", e);
        }
    }

    List<AppliedFactor> evaluateFactors(AssessRequest req, String vehicleCategory, int currentYear) {
        List<AppliedFactor> applied = new ArrayList<>();
        Map<String, RiskFactor> factorMap = new HashMap<>();
        for (RiskFactor f : cachedFactors) {
            factorMap.put(f.getFactorKey(), f);
        }

        checkAndAdd(factorMap, applied, "driver_age_under_25", req.getDriverAge() < 25);
        checkAndAdd(factorMap, applied, "driver_age_over_70",  req.getDriverAge() > 70);
        checkAndAdd(factorMap, applied, "license_under_2yr",   req.getLicenseYears() < 2);
        checkAndAdd(factorMap, applied, "violation_count_1",   req.getViolationsLast5Yr() == 1);
        checkAndAdd(factorMap, applied, "violation_count_2plus", req.getViolationsLast5Yr() >= 2);
        checkAndAdd(factorMap, applied, "accident_count_1",    req.getAccidentsLast5Yr() == 1);
        checkAndAdd(factorMap, applied, "accident_count_2plus", req.getAccidentsLast5Yr() >= 2);
        checkAndAdd(factorMap, applied, "vehicle_sports",      "sports".equals(vehicleCategory));
        checkAndAdd(factorMap, applied, "vehicle_luxury",      "luxury".equals(vehicleCategory));

        int vehicleAge = (req.getVehicleYear() != null) ? (currentYear - req.getVehicleYear()) : 0;
        checkAndAdd(factorMap, applied, "vehicle_age_over_15", vehicleAge > 15);

        String coverage = req.getCoverageType() != null ? req.getCoverageType().toUpperCase() : "";
        checkAndAdd(factorMap, applied, "coverage_comprehensive", "COMPREHENSIVE".equals(coverage));
        checkAndAdd(factorMap, applied, "coverage_collision",     "COLLISION".equals(coverage));
        checkAndAdd(factorMap, applied, "coverage_full",          "FULL".equals(coverage));
        checkAndAdd(factorMap, applied, "high_density_zip",
                req.getZipCode() != null && HIGH_DENSITY_ZIPS.contains(req.getZipCode().trim()));

        return applied;
    }

    private void checkAndAdd(Map<String, RiskFactor> factorMap, List<AppliedFactor> applied,
                              String key, boolean condition) {
        if (condition && factorMap.containsKey(key)) {
            RiskFactor f = factorMap.get(key);
            applied.add(AppliedFactor.builder()
                    .key(f.getFactorKey())
                    .label(f.getFactorLabel())
                    .scoreImpact(f.getBaseScoreImpact())
                    .multiplier(f.getPremiumMultiplier())
                    .build());
        }
    }

    String assignRiskTier(double score) {
        if (score <= 25.0) return "LOW";
        if (score <= 50.0) return "MEDIUM";
        if (score <= 75.0) return "HIGH";
        return "VERY_HIGH";
    }

    BigDecimal fetchBaseRate(String vehicleCategory, String stateCode) {
        try {
            Map<?, ?> response = ratesWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rates")
                            .queryParam("vehicleCategory", vehicleCategory)
                            .queryParamIfPresent("state", Optional.ofNullable(stateCode))
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("baseAnnualPremium")) {
                throw new RiskScoringException("Invalid response from rates-service");
            }
            return new BigDecimal(response.get("baseAnnualPremium").toString());
        } catch (WebClientResponseException e) {
            log.error("Rates service error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RiskScoringException("Failed to fetch base rate from rates-service: " + e.getMessage(), e);
        }
    }

    BigDecimal calculatePremium(BigDecimal baseRate, List<AppliedFactor> factors) {
        BigDecimal premium = baseRate;
        for (AppliedFactor factor : factors) {
            premium = premium.multiply(factor.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
        }
        return premium;
    }

    public String resolveVehicleCategory(AssessRequest request) {
        if (request.getVehicleCategory() != null && !request.getVehicleCategory().isBlank()) {
            return request.getVehicleCategory().toLowerCase().trim();
        }
        return detectVehicleCategory(request.getVehicleMake(), request.getVehicleModel());
    }

    String detectVehicleCategory(String make, String model) {
        String makeLower  = make  != null ? make.toLowerCase().trim()  : "";
        String modelLower = model != null ? model.toLowerCase().trim() : "";

        if (ELECTRIC_MAKES.contains(makeLower)) return "electric";
        if (containsAny(modelLower, ELECTRIC_MODELS)) return "electric";
        if (containsAny(modelLower, SPORTS_MODELS)) return "sports";
        if (LUXURY_MAKES.contains(makeLower)) return "luxury";
        if (containsAny(modelLower, LUXURY_MODELS)) return "luxury";
        if (containsAny(modelLower, TRUCK_MODELS)) return "truck";
        if (containsAny(modelLower, SUV_MODELS)) return "suv";
        return "sedan";
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
