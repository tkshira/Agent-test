package com.example.aiproducts.repository.impl;

import com.example.aiproducts.model.PurchaseLog;
import com.example.aiproducts.repository.PurchaseLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory purchase log repository.
 * Data is lost on restart — suitable for local dev and MCP log-only mode.
 *
 * Replace with a JPA / Cosmos DB implementation for production persistence.
 */
@Slf4j
@Repository
public class InMemoryPurchaseLogRepository implements PurchaseLogRepository {

    private final Map<String, PurchaseLog> store = new ConcurrentHashMap<>();

    @Override
    public PurchaseLog save(PurchaseLog purchaseLog) {
        store.put(purchaseLog.getId(), purchaseLog);
        log.info("[PurchaseLog] Saved: id={}, product={}, qty={}, status={}",
                purchaseLog.getId(),
                purchaseLog.getProductName(),
                purchaseLog.getQuantity(),
                purchaseLog.getStatus());
        return purchaseLog;
    }

    @Override
    public Optional<PurchaseLog> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<PurchaseLog> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<PurchaseLog> findBySessionId(String sessionId) {
        return store.values().stream()
                .filter(p -> sessionId.equals(p.getSessionId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseLog> findByProductId(String productId) {
        return store.values().stream()
                .filter(p -> productId.equals(p.getProductId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseLog> findByStatus(String status) {
        return store.values().stream()
                .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseLog> findBetween(Instant from, Instant to) {
        return store.values().stream()
                .filter(p -> !p.getTimestamp().isBefore(from) && !p.getTimestamp().isAfter(to))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
