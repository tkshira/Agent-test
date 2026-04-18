package com.example.aiproducts.service.impl;

import com.azure.search.documents.SearchClient;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.models.SemanticSearchOptions;
import com.example.aiproducts.model.Product;
import com.example.aiproducts.repository.ProductRepository;
import com.example.aiproducts.service.SearchService;
import com.example.aiproducts.config.AzureAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Azure AI Search implementation of SearchService.
 *
 * Performs semantic + keyword search against the products index.
 * Falls back to the in-memory ProductRepository if Azure Search is unavailable.
 *
 * Index schema expected in Azure AI Search:
 * - id (Edm.String, key)
 * - name (Edm.String, searchable)
 * - description (Edm.String, searchable)
 * - category (Edm.String, filterable, facetable)
 * - price (Edm.Double, filterable, sortable)
 * - currency (Edm.String)
 * - availability (Edm.String, filterable)
 * - rating (Edm.Double, sortable)
 * - tagline (Edm.String, searchable)
 * - imageUrl (Edm.String)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureSearchServiceImpl implements SearchService {

    private final SearchClient searchClient;
    private final ProductRepository productRepository;
    private final AzureAIConfig azureAIConfig;

    @Override
    public List<Product> searchProducts(String query, int topK) {
        try {
            SearchOptions options = new SearchOptions()
                    .setTop(topK)
                    .setIncludeTotalCount(false)
                    // Enable semantic ranking (requires semantic configuration in the index)
                    .setQueryType(com.azure.search.documents.models.QueryType.SEMANTIC)
                    .setSemanticSearchOptions(azureAIConfig.getSemanticSearchOptions());

            List<Product> results = new ArrayList<>();
            searchClient.search(query, options, null)
                    .forEach(result -> results.add(mapToProduct(result)));

            log.debug("Azure AI Search returned {} results for query: {}", results.size(), query);
            return results;

        } catch (Exception e) {
            log.warn("Azure AI Search unavailable, falling back to in-memory repository. Reason: {}", e.getMessage());
            return productRepository.search(query);
        }
    }

    @Override
    public Product getProductById(String productId) {
        try {
            Map<String, Object> doc = searchClient.getDocument(productId, Map.class);
            return mapDocumentToProduct(doc);
        } catch (Exception e) {
            log.warn("Product {} not found in Azure Search, checking local repo. Reason: {}", productId, e.getMessage());
            return productRepository.findById(productId).orElse(null);
        }
    }

    @Override
    public List<Product> listByCategory(String category, int topK) {
        try {
            SearchOptions options = new SearchOptions()
                    .setTop(topK)
                    .setFilter("category eq '" + category.replace("'", "''") + "'");

            List<Product> results = new ArrayList<>();
            searchClient.search("*", options, null)
                    .forEach(result -> results.add(mapToProduct(result)));

            return results;

        } catch (Exception e) {
            log.warn("Azure AI Search unavailable for category filter, falling back. Reason: {}", e.getMessage());
            return productRepository.findByCategory(category);
        }
    }

    @SuppressWarnings("unchecked")
    private Product mapToProduct(SearchResult result) {
        Map<String, Object> doc = (Map<String, Object>) result.getDocument(Map.class);
        return mapDocumentToProduct(doc);
    }

    private Product mapDocumentToProduct(Map<String, Object> doc) {
        Object priceObj = doc.get("price");
        BigDecimal price = priceObj != null
                ? new BigDecimal(priceObj.toString())
                : BigDecimal.ZERO;

        Object ratingObj = doc.get("rating");
        Double rating = ratingObj != null ? Double.parseDouble(ratingObj.toString()) : null;

        return Product.builder()
                .id(str(doc, "id"))
                .name(str(doc, "name"))
                .description(str(doc, "description"))
                .category(str(doc, "category"))
                .price(price)
                .currency(str(doc, "currency"))
                .availability(str(doc, "availability"))
                .imageUrl(str(doc, "imageUrl"))
                .rating(rating)
                .tagline(str(doc, "tagline"))
                .build();
    }

    private String str(Map<String, Object> doc, String key) {
        Object val = doc.get(key);
        return val != null ? val.toString() : null;
    }
}
