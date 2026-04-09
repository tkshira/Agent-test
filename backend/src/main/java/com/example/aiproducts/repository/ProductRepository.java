package com.example.aiproducts.repository;

import com.example.aiproducts.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for product data.
 *
 * Implementations can be swapped without changing service logic:
 * - In-memory (dev/testing)
 * - Azure SQL / PostgreSQL via Spring Data JPA
 * - Azure Cosmos DB
 * - Azure AI Search (read-only, for catalog browsing)
 */
public interface ProductRepository {

    Optional<Product> findById(String id);

    List<Product> findAll();

    List<Product> findByCategory(String category);

    /**
     * Full-text search by keyword (name, description, etc.).
     * Implementations backed by Azure AI Search provide semantic/vector search.
     */
    List<Product> search(String keyword);

    Product save(Product product);

    void deleteById(String id);
}
