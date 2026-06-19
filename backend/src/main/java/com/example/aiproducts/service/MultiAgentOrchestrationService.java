package com.example.aiproducts.service;

import com.example.aiproducts.model.ChatResponse;

/**
 * Orchestrates a pool of specialized agents.
 *
 * A keyword-based router classifies the user's intent and delegates to the
 * appropriate specialist (ProductExpert, PurchaseAdvisor, or Support).
 * Each specialist is a separate Azure AI Foundry agent with tailored instructions.
 */
public interface MultiAgentOrchestrationService {

    /**
     * Route the message to the best-fit specialist agent and return its response.
     *
     * @param message  user's input text
     * @param threadId existing conversation thread; null to start a new one
     * @return response from the specialist, including the role that handled it
     */
    ChatResponse orchestrate(String message, String threadId);
}
