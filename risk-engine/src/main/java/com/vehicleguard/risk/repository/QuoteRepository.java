package com.vehicleguard.risk.repository;

import com.vehicleguard.risk.model.entity.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<QuoteRequest, UUID> {
}
