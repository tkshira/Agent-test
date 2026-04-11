package com.example.aiproducts.service.impl;

import com.azure.ai.agents.persistent.PersistentAgentsClient;
import com.azure.ai.agents.persistent.models.AISearchIndexResource;
import com.azure.ai.agents.persistent.models.AzureAISearchToolDefinition;
import com.azure.ai.agents.persistent.models.AzureAISearchToolResource;
import com.azure.ai.agents.persistent.models.CreateAgentOptions;
import com.azure.ai.agents.persistent.models.CreateRunOptions;
import com.azure.ai.agents.persistent.models.ListSortOrder;
import com.azure.ai.agents.persistent.models.MessageContent;
import com.azure.ai.agents.persistent.models.MessageRole;
import com.azure.ai.agents.persistent.models.MessageTextContent;
import com.azure.ai.agents.persistent.models.PersistentAgent;
import com.azure.ai.agents.persistent.models.PersistentAgentThread;
import com.azure.ai.agents.persistent.models.RunStatus;
import com.azure.ai.agents.persistent.models.ThreadMessage;
import com.azure.ai.agents.persistent.models.ThreadRun;
import com.azure.ai.agents.persistent.models.ToolResources;
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
 * Uses the azure-ai-agents-persistent SDK to:
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

    private final PersistentAgentsClient agentsClient;
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
        if (configuredAgentId != null && !configuredAgentId.isBlank()) {
            agentId = configuredAgentId;
            log.info("Reusing Azure AI Foundry agent: {}", agentId);
            return;
        }

        // Wire up Azure AI Search as a grounding tool
        AISearchIndexResource indexResource = new AISearchIndexResource()
                .setIndexConnectionId(searchConnectionId)
                .setIndexName(searchIndexName);

        AzureAISearchToolResource searchToolResource = new AzureAISearchToolResource()
                .setIndexList(List.of(indexResource));

        ToolResources toolResources = new ToolResources()
                .setAzureAISearch(searchToolResource);

        // Create agent with search tool definition + resources
        PersistentAgent agent = agentsClient.getPersistentAgentsAdministrationClient()
                .createAgent(new CreateAgentOptions(model)
                        .setName(agentName)
                        .setInstructions(agentInstructions)
                        .setTools(List.of(new AzureAISearchToolDefinition()))
                        .setToolResources(toolResources));

        agentId = agent.getId();
        log.info("Created Azure AI Foundry agent: {} (id={}). " +
                 "Set AZURE_AGENT_ID={} to reuse across restarts.", agentName, agentId, agentId);
    }

    @Override
    public ChatResponse chat(String message, String threadId) {
        var threadsClient  = agentsClient.getThreadsClient();
        var messagesClient = agentsClient.getMessagesClient();
        var runsClient     = agentsClient.getRunsClient();

        // Create or retrieve thread
        PersistentAgentThread thread;
        if (threadId == null || threadId.isBlank()) {
            thread = threadsClient.createThread();
            log.debug("Created new thread: {}", thread.getId());
        } else {
            thread = threadsClient.getThread(threadId);
            log.debug("Reusing thread: {}", thread.getId());
        }

        // Add user message to thread
        messagesClient.createMessage(thread.getId(), MessageRole.USER, message);

        // Start the agent run and poll until a terminal state is reached
        ThreadRun run = runsClient.createRun(new CreateRunOptions(thread.getId(), agentId));
        run = pollUntilDone(runsClient, thread.getId(), run);

        if (!RunStatus.COMPLETED.equals(run.getStatus())) {
            log.error("Agent run ended with status: {}", run.getStatus());
            return ChatResponse.builder()
                    .message("I encountered an issue processing your request. Please try again.")
                    .threadId(thread.getId())
                    .error(true)
                    .build();
        }

        // Retrieve the latest assistant message
        String responseText = extractLatestAssistantMessage(messagesClient, thread.getId());

        // Enrich the response with structured product data
        List<Product> products = extractProductsFromResponse(message);

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
            agentsClient.getThreadsClient().deleteThread(threadId);
            log.debug("Deleted thread: {}", threadId);
        } catch (Exception e) {
            log.warn("Failed to delete thread {}: {}", threadId, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Polls getRun() every second until the run reaches a terminal state.
     */
    private ThreadRun pollUntilDone(
            com.azure.ai.agents.persistent.RunsClient runsClient,
            String threadId,
            ThreadRun run) {
        while (RunStatus.QUEUED.equals(run.getStatus())
                || RunStatus.IN_PROGRESS.equals(run.getStatus())
                || RunStatus.REQUIRES_ACTION.equals(run.getStatus())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            run = runsClient.getRun(threadId, run.getId());
        }
        return run;
    }

    private String extractLatestAssistantMessage(
            com.azure.ai.agents.persistent.MessagesClient messagesClient,
            String threadId) {

        // List messages descending (latest first), limit 1, no run filter
        List<ThreadMessage> messages = messagesClient
                .listMessages(threadId, null, 1, ListSortOrder.DESCENDING, null, null)
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
        return sb.isEmpty() ? "No response received." : sb.toString();
    }

    /**
     * After the agent replies, enrich the response with structured product data
     * by searching for products related to the user's query.
     */
    private List<Product> extractProductsFromResponse(String userMessage) {
        try {
            return searchService.searchProducts(userMessage, 4);
        } catch (Exception e) {
            log.warn("Product extraction failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
