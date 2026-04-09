package com.example.aiproducts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a fictional product returned from Azure AI Search.
 * Fields map to the search index schema — update to match your actual index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    private String id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String currency;

    /** e.g. "In Stock", "Out of Stock", "Pre-order" */
    private String availability;

    /** URL to product image (optional) */
    private String imageUrl;

    /** Star rating 0-5 */
    private Double rating;

    /** Short marketing tagline */
    private String tagline;
}
