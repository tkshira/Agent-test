package com.example.aiproducts.service;

import com.example.aiproducts.model.PurchaseLog;

import java.util.List;

/**
 * Abstraction over the purchase registration workflow.
 *
 * Current implementation: delegates to the embedded MCP server's log_purchase tool,
 * then persists via PurchaseLogRepository.
 *
 * Future: add payment gateway integration, order management system, inventory checks.
 */
public interface PurchaseService {

    /**
     * Register a purchase intent.
     * Calls the MCP log_purchase tool and persists the log entry.
     *
     * @param productId  product being purchased
     * @param quantity   quantity requested
     * @param sessionId  conversation session for traceability
     * @return the persisted purchase log entry
     */
    PurchaseLog registerPurchase(String productId, int quantity, String sessionId);

    /**
     * Get all purchase logs — useful for admin views or reporting.
     */
    List<PurchaseLog> getAllPurchases();

    /**
     * Get purchase history for a specific session.
     */
    List<PurchaseLog> getPurchasesBySession(String sessionId);
}
