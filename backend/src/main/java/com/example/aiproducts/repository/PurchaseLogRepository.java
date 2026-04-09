package com.example.aiproducts.repository;

import com.example.aiproducts.model.PurchaseLog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for purchase log persistence.
 *
 * Swap implementations to change the storage backend:
 * - InMemoryPurchaseLogRepository  — local dev / MCP logging
 * - JpaPurchaseLogRepository        — Spring Data JPA (any relational DB)
 * - CosmosPurchaseLogRepository     — Azure Cosmos DB
 * - BlobPurchaseLogRepository       — Azure Blob Storage (append-only audit log)
 */
public interface PurchaseLogRepository {

    PurchaseLog save(PurchaseLog purchaseLog);

    Optional<PurchaseLog> findById(String id);

    List<PurchaseLog> findAll();

    List<PurchaseLog> findBySessionId(String sessionId);

    List<PurchaseLog> findByProductId(String productId);

    List<PurchaseLog> findByStatus(String status);

    List<PurchaseLog> findBetween(Instant from, Instant to);

    void deleteById(String id);
}
