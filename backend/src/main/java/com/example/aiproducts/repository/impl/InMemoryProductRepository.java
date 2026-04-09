package com.example.aiproducts.repository.impl;

import com.example.aiproducts.model.Product;
import com.example.aiproducts.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory product repository seeded with fictional products.
 * Replace with a JPA/Cosmos/Search-backed implementation for production.
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> store = new ConcurrentHashMap<>();

    public InMemoryProductRepository() {
        seed();
    }

    @Override
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Product> findByCategory(String category) {
        return store.values().stream()
                .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> search(String keyword) {
        String lower = keyword.toLowerCase();
        return store.values().stream()
                .filter(p -> contains(p.getName(), lower)
                        || contains(p.getDescription(), lower)
                        || contains(p.getCategory(), lower)
                        || contains(p.getTagline(), lower))
                .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        store.put(product.getId(), product);
        return product;
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    private boolean contains(String field, String keyword) {
        return field != null && field.toLowerCase().contains(keyword);
    }

    private void seed() {
        List<Product> products = List.of(
                Product.builder()
                        .id("P001")
                        .name("NebulaPhone X")
                        .description("Flagship smartphone with quantum-dot display, 200MP camera system, and 7-day battery.")
                        .category("Electronics")
                        .price(new BigDecimal("1299.99"))
                        .currency("USD")
                        .availability("In Stock")
                        .rating(4.8)
                        .tagline("The phone from the future.")
                        .build(),
                Product.builder()
                        .id("P002")
                        .name("AeroChair Pro")
                        .description("Ergonomic mesh office chair with lumbar AI adjustment and 4D armrests.")
                        .category("Furniture")
                        .price(new BigDecimal("899.00"))
                        .currency("USD")
                        .availability("In Stock")
                        .rating(4.6)
                        .tagline("Work smarter, sit better.")
                        .build(),
                Product.builder()
                        .id("P003")
                        .name("HyperBrew Espresso Station")
                        .description("Smart espresso machine with bean-to-cup brewing, cloud recipes, and voice control.")
                        .category("Kitchen")
                        .price(new BigDecimal("549.00"))
                        .currency("USD")
                        .availability("In Stock")
                        .rating(4.9)
                        .tagline("Barista-grade coffee, effortlessly.")
                        .build(),
                Product.builder()
                        .id("P004")
                        .name("SkyPod Headphones")
                        .description("Over-ear headphones with spatial audio, 48h battery, and adaptive ANC.")
                        .category("Electronics")
                        .price(new BigDecimal("349.99"))
                        .currency("USD")
                        .availability("In Stock")
                        .rating(4.7)
                        .tagline("Hear everything. Be everywhere.")
                        .build(),
                Product.builder()
                        .id("P005")
                        .name("LunaGlow Smart Lamp")
                        .description("AI-powered circadian rhythm lamp that adjusts to your sleep schedule and mood.")
                        .category("Home")
                        .price(new BigDecimal("129.00"))
                        .currency("USD")
                        .availability("In Stock")
                        .rating(4.5)
                        .tagline("Light that thinks for you.")
                        .build(),
                Product.builder()
                        .id("P006")
                        .name("VeloRun X3 Smart Shoes")
                        .description("Running shoes with embedded sensors tracking cadence, form, and terrain in real time.")
                        .category("Sports")
                        .price(new BigDecimal("289.00"))
                        .currency("USD")
                        .availability("Pre-order")
                        .rating(4.4)
                        .tagline("Every step, optimized.")
                        .build(),
                Product.builder()
                        .id("P007")
                        .name("CrystalGuard Monitor 27\"")
                        .description("27-inch 4K OLED monitor with 240Hz refresh, HDR1000, and zero-latency gaming mode.")
                        .category("Electronics")
                        .price(new BigDecimal("799.00"))
                        .currency("USD")
                        .availability("In Stock")
                        .rating(4.8)
                        .tagline("Visuals redefined.")
                        .build(),
                Product.builder()
                        .id("P008")
                        .name("ZenPack Travel Backpack")
                        .description("30L anti-theft backpack with solar charging panel, RFID blocking, and modular compartments.")
                        .category("Travel")
                        .price(new BigDecimal("199.00"))
                        .currency("USD")
                        .availability("In Stock")
                        .rating(4.6)
                        .tagline("Your world, carried smarter.")
                        .build()
        );

        products.forEach(p -> store.put(p.getId(), p));
    }
}
