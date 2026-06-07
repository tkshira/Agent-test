package com.example.aiproducts.service.impl;

import com.azure.ai.agents.persistent.PersistentAgentsClient;
import com.azure.ai.agents.persistent.models.AISearchIndexResource;
import com.azure.ai.agents.persistent.models.AzureAISearchQueryType;
import com.azure.ai.projects.ConnectionsClient;
import com.azure.ai.projects.models.ConnectionType;
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
import java.util.Optional;

/**
 * Azure AI Foundry implementation of AgentService.
 *
 * On startup, looks for an existing agent whose name matches azure.ai.agent.name
 * and reuses it. Creates a new one only when no match is found. This avoids
 * accumulating duplicate agents across restarts without any file or env-var
 * management. Set AZURE_AGENT_ID to pin a specific agent ID when needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureAgentServiceImpl implements AgentService {

    private final PersistentAgentsClient agentsClient;
    private final ConnectionsClient connectionsClient;
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

    @Value("${azure.ai.search.query-type:simple}")
    private String searchQueryType;

    private String agentId;

    @PostConstruct
    public void initAgent() {
        var adminClient = agentsClient.getPersistentAgentsAdministrationClient();

        // Explicit override: honour a pinned ID when one is configured
        if (configuredAgentId != null && !configuredAgentId.isBlank()) {
            agentId = configuredAgentId;
            log.info("Using pinned agent id from config: {}", agentId);
            return;
        }

        // Default: find the first existing agent whose name matches our configured name.
        // This avoids creating duplicate agents on every restart without any file or
        // env-var management.
        Optional<PersistentAgent> existing = adminClient.listAgents()
                .stream()
                .filter(a -> agentName.equals(a.getName()))
                .findFirst();

        if (existing.isPresent()) {
            agentId = existing.get().getId();
            log.info("Reusing existing agent '{}' (id={})", agentName, agentId);
            return;
        }

        // No agent with this name found — create one
        String resolvedConnectionId = resolveConnectionId(searchConnectionId);
        AzureAISearchQueryType queryType = AzureAISearchQueryType.fromString(searchQueryType);

        AISearchIndexResource indexResource = new AISearchIndexResource()
                .setIndexConnectionId(resolvedConnectionId)
                .setIndexName(searchIndexName)
                .setQueryType(queryType);

        ToolResources toolResources = new ToolResources()
                .setAzureAISearch(new AzureAISearchToolResource()
                        .setIndexList(List.of(indexResource)));

        PersistentAgent agent = adminClient.createAgent(new CreateAgentOptions(model)
                .setName(agentName)
                .setInstructions(agentInstructions)
                .setTools(List.of(new AzureAISearchToolDefinition()))
                .setToolResources(toolResources));

        agentId = agent.getId();
        log.info("Created new agent '{}' (id={})", agentName, agentId);
    }

    /**
     * Returns the full ARM connection ID needed by both the agent runtime and the portal.
     * If the configured value is already an absolute path (starts with "/subscriptions/"),
     * it is used as-is. Otherwise it is treated as a connection name and resolved via the
     * Connections API, which is the same approach the Python SDK sample uses:
     *   project_client.connections.get_default(ConnectionType.AZURE_AI_SEARCH).id
     */
    private String resolveConnectionId(String configured) {
        if (configured != null && configured.startsWith("/subscriptions/")) {
            log.debug("Using configured connection ID directly: {}", configured);
            return configured;
        }
        try {
            if (configured != null && !configured.isBlank()) {
                String resolved = connectionsClient.getConnection(configured, false).getId();
                log.info("Resolved search connection '{}' → {}", configured, resolved);
                return resolved;
            }
            // Fall back to the default Azure AI Search connection registered in the project
            String resolved = connectionsClient
                    .getDefaultConnection(ConnectionType.AZURE_AISEARCH, false)
                    .getId();
            log.info("Resolved default Azure AI Search connection → {}", resolved);
            return resolved;
        } catch (Exception e) {
            log.warn("Could not resolve connection ID for '{}', using as-is: {}", configured, e.getMessage());
            return configured;
        }
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
        // messagesClient.createMessage(thread.getId(), MessageRole.USER, message);

        // // Start the agent run and poll until a terminal state is reached
        // ThreadRun run = runsClient.createRun(new CreateRunOptions(thread.getId(), agentId));
        // run = pollUntilDone(runsClient, thread.getId(), run);

        // if (!RunStatus.COMPLETED.equals(run.getStatus())) {
        //     log.error("Agent run ended with status: {}", run.getStatus());
        //     return ChatResponse.builder()
        //             .message("I encountered an issue processing your request. Please try again.")
        //             .threadId(thread.getId())
        //             .error(true)
        //             .build();
        // }

        // // Retrieve the latest assistant message
        // String responseText = extractLatestAssistantMessage(messagesClient, thread.getId());
        String responseText = "Simulated agent response to: " + message;
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
