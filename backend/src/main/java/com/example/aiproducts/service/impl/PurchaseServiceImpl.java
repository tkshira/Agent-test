package com.example.aiproducts.service.impl;

import com.example.aiproducts.mcp.McpServer;
import com.example.aiproducts.model.Product;
import com.example.aiproducts.model.PurchaseLog;
import com.example.aiproducts.repository.PurchaseLogRepository;
import com.example.aiproducts.service.PurchaseService;
import com.example.aiproducts.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final McpServer mcpServer;
    private final PurchaseLogRepository purchaseLogRepository;
    private final SearchService searchService;

    @Override
    public PurchaseLog registerPurchase(String productId, int quantity, String sessionId) {
        // 1. Resolve product details
        Product product = searchService.getProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        // 2. Call the MCP log_purchase tool — this is the MCP integration point
        Map<String, Object> mcpParams = Map.of(
                "product_id",   productId,
                "product_name", product.getName(),
                "price",        product.getPrice().toPlainString(),
                "currency",     product.getCurrency(),
                "quantity",     quantity,
                "session_id",   sessionId != null ? sessionId : "anonymous"
        );

        Object mcpResult = mcpServer.callTool("log_purchase", mcpParams);
        log.info("MCP log_purchase result: {}", mcpResult);

        // 3. Persist via repository (in-memory now; swap for DB later)
        PurchaseLog log = PurchaseLog.builder()
                .productId(productId)
                .productName(product.getName())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .quantity(quantity)
                .sessionId(sessionId)
                .status("PENDING")
                .build();

        return purchaseLogRepository.save(log);
    }

    @Override
    public List<PurchaseLog> getAllPurchases() {
        return purchaseLogRepository.findAll();
    }

    @Override
    public List<PurchaseLog> getPurchasesBySession(String sessionId) {
        return purchaseLogRepository.findBySessionId(sessionId);
    }
}
