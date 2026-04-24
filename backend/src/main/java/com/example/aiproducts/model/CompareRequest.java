package com.example.aiproducts.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CompareRequest {

    @NotEmpty(message = "productIds must not be empty")
    @Size(min = 2, max = 4, message = "Provide between 2 and 4 product IDs to compare")
    private List<String> productIds;
}
