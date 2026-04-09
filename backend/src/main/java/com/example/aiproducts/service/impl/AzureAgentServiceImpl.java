package com.example.aiproducts.service.impl;

import com.azure.ai.projects.AIProjectClient;
import com.azure.ai.projects.models.*;
import com.example.aiproducts.model.ChatResponse;
import com.example.aiproducts.model.Product;
import com.example.aiproducts.service.AgentService;
import com.example.aiproducts.service.SearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Azure AI Foundry implementation of AgentService.
 *
 * Uses the azure-ai-projects SDK to:
 * 1. Create/reuse an agent with AzureAISearch tool grounding
 * 2. Manage conversation threads
 * 3. Parse product references from agent responses
 *
 * The agent is created once on startup. Its ID is logged so you can persist it
 * in AZURE_AGENT_ID to reuse across restarts (avoiding creating duplicate agents).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureAgentServiceImpl implements AgentService {

    private final AIProjectClient projectClient;
    private final SearchService searchService;

    @Value("${azure.ai.agent.model}")
    private String model;

    @Value("${azure.ai.agent.name}")
    private String agentName;

    @Value("${azure.ai.agent.instructions}")
    private String agentInstructions;

    @Value("${azure.ai.agent.id:}")
    private String configuredAgentId;

    @Value("${azure.ai.search.connection-id}")
    private String searchConnectionId;

    @Value("${azure.ai.search.index-name}")
    private String searchIndexName;

    private String agentId;

    @PostConstruct
    public void initAgent() {
        AgentsClient agentsClient = projectClient.getAgentsClient();

        if (configuredAgentId != null && !configuredAgentId.isBlank()) {
            // Reuse existing agent
            agentId = configuredAgentId;
            log.info("Reusing Azure AI Foundry agent: {}", agentId);
            return;
        }

        // Build the Azure AI Search tool definition for grounding
        AzureAISearchToolDefinition searchTool = new AzureAISearchToolDefinition();
        AzureAISearchIndexResource indexResource = new AzureAISearchIndexResource(
                searchConnectionId, searchIndexName);
        searchTool.setIndexList(List.of(indexResource));

        // Create agent with search tool
        Agent agent = agentsClient.createAgent(
                new CreateAgentOptions(model)
                        .setName(agentName)
                        .setInstructions(agentInstructions)
                        .setTools(List.of(searchTool))
        );

        agentId = agent.getId();
        log.info("Created Azure AI Foundry agent: {} (id={}). " +
                 "Set AZURE_AGENT_ID={} to reuse across restarts.", agentName, agentId, agentId);
    }

    @Override
    public ChatResponse chat(String message, String threadId) {
        AgentsClient agentsClient = projectClient.getAgentsClient();

        // Create or retrieve thread
        AgentThread thread;
        if (threadId == null || threadId.isBlank()) {
            thread = agentsClient.createThread(new AgentThreadCreationOptions());
            log.debug("Created new thread: {}", thread.getId());
        } else {
            thread = agentsClient.getThread(threadId);
            log.debug("Reusing thread: {}", thread.getId());
        }

        // Add user message to thread
        agentsClient.createMessage(
                thread.getId(),
                new CreateMessageOptions(MessageRole.USER)
                        .setContent(message)
        );

        // Run the agent on the thread
        ThreadRun run = agentsClient.createAndProcessRun(
                thread.getId(),
                new CreateRunOptions(agentId)
        );

        if (run.getStatus() != RunStatus.COMPLETED) {
            log.error("Agent run ended with status: {}", run.getStatus());
            return ChatResponse.builder()
                    .message("I encountered an issue processing your request. Please try again.")
                    .threadId(thread.getId())
                    .error(true)
                    .build();
        }

        // Retrieve the latest assistant message
        String responseText = extractLatestAssistantMessage(agentsClient, thread.getId());

        // Parse any product references from the response
        List<Product> products = extractProductsFromResponse(responseText, message);

        return ChatResponse.builder()
                .message(responseText)
                .products(products)
                .threadId(thread.getId())
                .build();
    }

    @Override
    public void deleteThread(String threadId) {
        if (threadId == null || threadId.isBlank()) return;
        try {
            projectClient.getAgentsClient().deleteThread(threadId);
            log.debug("Deleted thread: {}", threadId);
        } catch (Exception e) {
            log.warn("Failed to delete thread {}: {}", threadId, e.getMessage());
        }
    }

    private String extractLatestAssistantMessage(AgentsClient agentsClient, String threadId) {
        // List messages in descending order (latest first)
        List<ThreadMessage> messages = agentsClient
                .listMessages(threadId, new ListSortOrder("desc"), 1, null, null, null)
                .stream()
                .toList();

        if (messages.isEmpty()) {
            return "No response received.";
        }

        ThreadMessage latest = messages.get(0);
        StringBuilder sb = new StringBuilder();
        for (MessageContent content : latest.getContent()) {
            if (content instanceof MessageTextContent textContent) {
                sb.append(textContent.getText().getValue());
            }
        }
        return sb.toString();
    }

    /**
     * After the agent replies, optionally enrich the response with structured product data
     * by re-searching for products mentioned in the conversation.
     */
    private List<Product> extractProductsFromResponse(String responseText, String userMessage) {
        try {
            // Search for products based on the user's original query
            return searchService.searchProducts(userMessage, 4);
        } catch (Exception e) {
            log.warn("Product extraction failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
