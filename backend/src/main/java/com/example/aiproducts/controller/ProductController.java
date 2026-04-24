package com.example.aiproducts.controller;

import com.example.aiproducts.model.CompareRequest;
import com.example.aiproducts.model.Product;
import com.example.aiproducts.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final SearchService searchService;

    /**
     * GET /api/products/related/{productId}?topK=4
     *
     * Returns products related to the given product (same category / similar attributes).
     */
    @GetMapping("/related/{productId}")
    public ResponseEntity<List<Product>> getRelated(
            @PathVariable String productId,
            @RequestParam(defaultValue = "4") int topK) {

        log.debug("Related products request for productId={}", productId);
        List<Product> related = searchService.findRelated(productId, topK);
        return ResponseEntity.ok(related);
    }

    /**
     * POST /api/products/compare
     *
     * Returns the full product details for a list of product IDs so the
     * frontend can render a side-by-side comparison table.
     *
     * Request:  { "productIds": ["P001", "P004"] }
     * Response: [ { Product }, { Product }, ... ]
     */
    @PostMapping("/compare")
    public ResponseEntity<List<Product>> compare(@Valid @RequestBody CompareRequest request) {
        log.debug("Compare request for products: {}", request.getProductIds());
        List<Product> products = request.getProductIds().stream()
                .map(searchService::getProductById)
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.ok(products);
    }
}
