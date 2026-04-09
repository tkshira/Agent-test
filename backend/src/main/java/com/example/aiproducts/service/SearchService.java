package com.example.aiproducts.service;

import com.example.aiproducts.model.Product;

import java.util.List;

/**
 * Abstraction over the product search backend.
 *
 * Current implementation: Azure AI Search (semantic + keyword).
 * Future alternatives: Elasticsearch, Azure Cosmos DB vector search, PostgreSQL full-text.
 */
public interface SearchService {

    /**
     * Full-text / semantic search over the product catalog.
     *
     * @param query   natural language search phrase
     * @param topK    maximum number of results to return
     * @return ranked list of matching products
     */
    List<Product> searchProducts(String query, int topK);

    /**
     * Retrieve a specific product by its ID.
     *
     * @param productId the product's unique identifier
     * @return the product, or null if not found
     */
    Product getProductById(String productId);

    /**
     * List all products in a given category.
     *
     * @param category product category filter
     * @param topK     maximum results
     * @return matching products
     */
    List<Product> listByCategory(String category, int topK);
}
