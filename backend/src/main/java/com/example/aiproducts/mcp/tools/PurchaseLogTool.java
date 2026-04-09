package com.example.aiproducts.mcp.tools;

import com.example.aiproducts.mcp.McpTool;
import com.example.aiproducts.model.PurchaseLog;
import com.example.aiproducts.repository.PurchaseLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tool: log_purchase
 *
 * Logs a purchase intent to:
 * 1. The application log (always)
 * 2. A file on disk (configurable via mcp.server.log-path)
 * 3. The PurchaseLogRepository (in-memory now; swap for DB later)
 *
 * Input schema:
 * {
 *   "product_id":   string (required),
 *   "product_name": string (required),
 *   "price":        string (required, numeric string),
 *   "currency":     string (required),
 *   "quantity":     number (required),
 *   "session_id":   string (optional)
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseLogTool implements McpTool {

    private final PurchaseLogRepository purchaseLogRepository;

    @Value("${mcp.server.log-path:./logs/purchases.log}")
    private String logPath;

    @Override
    public String getName() {
        return "log_purchase";
    }

    @Override
    public String getDescription() {
        return "Registers a purchase intent. Logs the event to file and the purchase repository. " +
               "Call this whenever a user confirms they want to buy a product.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product_id",   property("string", "Unique product identifier"));
        properties.put("product_name", property("string", "Human-readable product name"));
        properties.put("price",        property("string", "Unit price as a numeric string"));
        properties.put("currency",     property("string", "ISO 4217 currency code, e.g. USD"));
        properties.put("quantity",     property("number", "Number of units to purchase"));
        properties.put("session_id",   property("string", "Conversation session identifier"));

        schema.put("properties", properties);
        schema.put("required", new String[]{"product_id", "product_name", "price", "currency", "quantity"});
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        String productId   = required(arguments, "product_id");
        String productName = required(arguments, "product_name");
        String priceStr    = required(arguments, "price");
        String currency    = required(arguments, "currency");
        int    quantity    = ((Number) arguments.getOrDefault("quantity", 1)).intValue();
        String sessionId   = (String) arguments.getOrDefault("session_id", "anonymous");

        BigDecimal price = new BigDecimal(priceStr);

        PurchaseLog entry = PurchaseLog.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .currency(currency)
                .quantity(quantity)
                .sessionId(sessionId)
                .status("PENDING")
                .timestamp(Instant.now())
                .build();

        // Persist to repository
        purchaseLogRepository.save(entry);

        // Append to log file
        writeToFile(entry);

        log.info("[MCP:log_purchase] id={} product='{}' qty={} price={} {} session={}",
                entry.getId(), productName, quantity, price, currency, sessionId);

        return Map.of(
                "success",      true,
                "purchase_id",  entry.getId(),
                "message",      "Purchase logged successfully",
                "timestamp",    entry.getTimestamp().toString()
        );
    }

    private void writeToFile(PurchaseLog entry) {
        try {
            Path path = Path.of(logPath);
            Files.createDirectories(path.getParent() == null ? Path.of(".") : path.getParent());

            String line = String.format("[%s] id=%s product_id=%s product_name=%s qty=%d price=%s %s session=%s status=%s%n",
                    entry.getTimestamp(),
                    entry.getId(),
                    entry.getProductId(),
                    entry.getProductName(),
                    entry.getQuantity(),
                    entry.getPrice().toPlainString(),
                    entry.getCurrency(),
                    entry.getSessionId(),
                    entry.getStatus());

            Files.writeString(path, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write purchase log to file {}: {}", logPath, e.getMessage());
        }
    }

    private String required(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) throw new IllegalArgumentException("Missing required argument: " + key);
        return val.toString();
    }

    private Map<String, Object> property(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
