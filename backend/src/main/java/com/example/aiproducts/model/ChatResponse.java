package com.example.aiproducts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** The agent's reply text (may contain markdown). */
    private String message;

    /**
     * Products extracted/identified from the agent response.
     * Populated when the agent found matching products in the catalog.
     */
    private List<Product> products;

    /**
     * Thread ID to pass back in subsequent requests for conversation continuity.
     */
    private String threadId;

    /** True when the response contains an error message. */
    @Builder.Default
    private boolean error = false;

    /**
     * The specialist agent that handled this response.
     * Null for single-agent (non-orchestrated) requests.
     */
    private AgentRole handledBy;
}
