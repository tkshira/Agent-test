package com.example.aiproducts.service;

import com.example.aiproducts.model.ChatResponse;

/**
 * Abstraction over any AI agent backend.
 *
 * Current implementation: Azure AI Foundry (azure-ai-projects SDK).
 * Future alternatives: OpenAI Assistants API, LangChain4j, custom LLM.
 */
public interface AgentService {

    /**
     * Send a message to the agent and get a response.
     *
     * @param message  the user's input
     * @param threadId existing conversation thread; null to start a new one
     * @return agent response including extracted products and the thread ID
     */
    ChatResponse chat(String message, String threadId);

    /**
     * Delete a conversation thread when the session ends.
     *
     * @param threadId the thread to clean up
     */
    void deleteThread(String threadId);
}
