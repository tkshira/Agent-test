package com.example.aiproducts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a purchase intent logged via the MCP server.
 * Persisted through PurchaseLogRepository — swap implementation for a real DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseLog {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String productId;
    private String productName;
    private BigDecimal price;
    private String currency;
    private int quantity;

    /** Session/conversation ID for traceability */
    private String sessionId;

    /** Optional: user identifier (anonymous until auth is added) */
    private String userId;

    @Builder.Default
    private Instant timestamp = Instant.now();

    /** "PENDING" | "CONFIRMED" | "FAILED" */
    @Builder.Default
    private String status = "PENDING";
}
