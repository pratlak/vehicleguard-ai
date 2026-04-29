package com.vehicleguard.rates.repository;

import com.vehicleguard.rates.model.entity.VehicleBaseRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleBaseRateRepository extends JpaRepository<VehicleBaseRate, Long> {
    Optional<VehicleBaseRate> findByVehicleCategory(String vehicleCategory);
}
