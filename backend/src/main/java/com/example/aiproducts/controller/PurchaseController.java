package com.example.aiproducts.controller;

import com.example.aiproducts.model.PurchaseLog;
import com.example.aiproducts.service.PurchaseService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * POST /api/purchase
     *
     * Register a purchase intent. Triggers the MCP log_purchase tool.
     *
     * Request:  { "productId": "P001", "quantity": 1, "sessionId": "thread_abc123" }
     * Response: { PurchaseLog }
     */
    @PostMapping
    public ResponseEntity<PurchaseLog> purchase(@Valid @RequestBody PurchaseRequest request) {
        log.info("Purchase request: productId={} qty={} session={}",
                request.getProductId(), request.getQuantity(), request.getSessionId());

        PurchaseLog log = purchaseService.registerPurchase(
                request.getProductId(),
                request.getQuantity(),
                request.getSessionId()
        );

        return ResponseEntity.ok(log);
    }

    /**
     * GET /api/purchase
     *
     * List all purchase logs (admin/dev endpoint).
     */
    @GetMapping
    public ResponseEntity<List<PurchaseLog>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    /**
     * GET /api/purchase/session/{sessionId}
     *
     * Get purchases for a specific conversation session.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<PurchaseLog>> getPurchasesBySession(@PathVariable String sessionId) {
        return ResponseEntity.ok(purchaseService.getPurchasesBySession(sessionId));
    }

    @Data
    public static class PurchaseRequest {
        @NotBlank
        private String productId;

        @Min(1)
        private int quantity = 1;

        private String sessionId;
    }
}
