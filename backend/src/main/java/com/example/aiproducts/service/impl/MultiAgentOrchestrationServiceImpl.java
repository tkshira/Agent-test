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
import com.example.aiproducts.model.AgentRole;
import com.example.aiproducts.model.ChatResponse;
import com.example.aiproducts.model.Product;
import com.example.aiproducts.service.MultiAgentOrchestrationService;
import com.example.aiproducts.service.SearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Routes each user message to a specialist Azure AI Foundry agent.
 *
 * Three specialists are created at startup (or reused via configured IDs):
 *   - ProductExpert    – grounded with Azure AI Search; handles catalog queries
 *   - PurchaseAdvisor  – guides buying decisions and order flow
 *   - Support          – handles issues, returns, and complaints
 *
 * Intent classification uses keyword matching so routing works in simulation
 * mode without Azure credentials.  When credentials are present, un-comment the
 * Azure run calls in {@link #runSpecialist} to activate real LLM responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiAgentOrchestrationServiceImpl implements MultiAgentOrchestrationService {

    private final PersistentAgentsClient agentsClient;
    private final SearchService searchService;

    @Value("${azure.ai.multi-agent.model:gpt-4o}")
    private String model;

    // ── Agent names ──────────────────────────────────────────────────────────
    @Value("${azure.ai.multi-agent.product-expert.name:ProductExpertAgent}")
    private String productExpertName;
    @Value("${azure.ai.multi-agent.purchase-advisor.name:PurchaseAdvisorAgent}")
    private String purchaseAdvisorName;
    @Value("${azure.ai.multi-agent.support.name:SupportAgent}")
    private String supportName;

    // ── Pre-configured IDs (set env vars to reuse agents across restarts) ────
    @Value("${azure.ai.multi-agent.product-expert.id:}")
    private String configuredProductExpertId;
    @Value("${azure.ai.multi-agent.purchase-advisor.id:}")
    private String configuredPurchaseAdvisorId;
    @Value("${azure.ai.multi-agent.support.id:}")
    private String configuredSupportId;

    // ── System prompts ───────────────────────────────────────────────────────
    @Value("${azure.ai.multi-agent.product-expert.instructions}")
    private String productExpertInstructions;
    @Value("${azure.ai.multi-agent.purchase-advisor.instructions}")
    private String purchaseAdvisorInstructions;
    @Value("${azure.ai.multi-agent.support.instructions}")
    private String supportInstructions;

    // ── Azure AI Search (for ProductExpert grounding) ────────────────────────
    @Value("${azure.ai.search.connection-id}")
    private String searchConnectionId;
    @Value("${azure.ai.search.index-name}")
    private String searchIndexName;

    private final Map<AgentRole, String> agentIds = new EnumMap<>(AgentRole.class);

    // ─────────────────────────────────────────────────────────────────────────
    // Startup
    // ─────────────────────────────────────────────────────────────────────────

    @PostConstruct
    void initAgents() {
        agentIds.put(AgentRole.PRODUCT_EXPERT,
                resolveOrCreate(configuredProductExpertId, productExpertName, productExpertInstructions, true));
        agentIds.put(AgentRole.PURCHASE_ADVISOR,
                resolveOrCreate(configuredPurchaseAdvisorId, purchaseAdvisorName, purchaseAdvisorInstructions, false));
        agentIds.put(AgentRole.SUPPORT,
                resolveOrCreate(configuredSupportId, supportName, supportInstructions, false));
    }

    private String resolveOrCreate(String configuredId, String name, String instructions, boolean withSearch) {
        if (configuredId != null && !configuredId.isBlank()) {
            log.info("Reusing {} agent: {}", name, configuredId);
            return configuredId;
        }

        CreateAgentOptions options = new CreateAgentOptions(model)
                .setName(name)
                .setInstructions(instructions);

        if (withSearch) {
            AISearchIndexResource index = new AISearchIndexResource()
                    .setIndexConnectionId(searchConnectionId)
                    .setIndexName(searchIndexName);
            options.setTools(List.of(new AzureAISearchToolDefinition()))
                   .setToolResources(new ToolResources()
                           .setAzureAISearch(new AzureAISearchToolResource()
                                   .setIndexList(List.of(index))));
        }

        PersistentAgent agent = agentsClient.getPersistentAgentsAdministrationClient()
                .createAgent(options);
        log.info("Created {} (id={}). Set AZURE_{}_AGENT_ID={} to reuse across restarts.",
                name, agent.getId(), name.toUpperCase().replace(" ", "_"), agent.getId());
        return agent.getId();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Orchestration
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChatResponse orchestrate(String message, String threadId) {
        AgentRole role = route(message);
        log.info("Routing '{}...' → {}", message.substring(0, Math.min(message.length(), 40)), role);
        return runSpecialist(role, message, threadId);
    }

    /**
     * Keyword-based router — classifies intent without an extra LLM call.
     * Package-private for unit testing.
     */
    AgentRole route(String message) {
        String lower = message.toLowerCase();
        // Check support keywords first — they are more specific and overlap with purchase terms
        // (e.g. "problem with my order" should route to Support, not PurchaseAdvisor)
        if (containsAny(lower, "help", "support", "issue", "problem", "broken",
                "return", "refund", "complaint", "defect", "wrong")) {
            return AgentRole.SUPPORT;
        }
        if (containsAny(lower, "buy", "purchase", "order", "checkout", "payment", "price", "cost", "how much")) {
            return AgentRole.PURCHASE_ADVISOR;
        }
        return AgentRole.PRODUCT_EXPERT;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Agent execution
    // ─────────────────────────────────────────────────────────────────────────

    private ChatResponse runSpecialist(AgentRole role, String message, String threadId) {
        var threadsClient  = agentsClient.getThreadsClient();
        var messagesClient = agentsClient.getMessagesClient();
        var runsClient     = agentsClient.getRunsClient();
        String specialistAgentId = agentIds.get(role);

        PersistentAgentThread thread = (threadId == null || threadId.isBlank())
                ? threadsClient.createThread()
                : threadsClient.getThread(threadId);

        // ── Activate when Azure credentials are available ─────────────────
        // messagesClient.createMessage(thread.getId(), MessageRole.USER, message);
        //
        // ThreadRun run = runsClient.createRun(new CreateRunOptions(thread.getId(), specialistAgentId));
        // run = pollUntilDone(runsClient, thread.getId(), run);
        //
        // if (!RunStatus.COMPLETED.equals(run.getStatus())) {
        //     log.error("{} run ended with status {}", role, run.getStatus());
        //     return ChatResponse.builder()
        //             .message("I encountered an issue. Please try again.")
        //             .threadId(thread.getId())
        //             .handledBy(role)
        //             .error(true)
        //             .build();
        // }
        // String responseText = extractLatestAssistantMessage(messagesClient, thread.getId());
        // ─────────────────────────────────────────────────────────────────────

        String responseText = buildSimulatedResponse(role, message);
        List<Product> products = role == AgentRole.PRODUCT_EXPERT
                ? extractProducts(message)
                : List.of();

        return ChatResponse.builder()
                .message(responseText)
                .products(products)
                .threadId(thread.getId())
                .handledBy(role)
                .build();
    }

    private String buildSimulatedResponse(AgentRole role, String message) {
        return switch (role) {
            case PRODUCT_EXPERT    -> "[ProductExpert] Searching our catalog for: " + message;
            case PURCHASE_ADVISOR  -> "[PurchaseAdvisor] Helping you with your purchase: " + message;
            case SUPPORT           -> "[Support] I'll help resolve your issue: " + message;
        };
    }

    private List<Product> extractProducts(String query) {
        try {
            return searchService.searchProducts(query, 4);
        } catch (Exception e) {
            log.warn("Product search failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers (used when Azure run is active)
    // ─────────────────────────────────────────────────────────────────────────

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
        List<ThreadMessage> messages = messagesClient
                .listMessages(threadId, null, 1, ListSortOrder.DESCENDING, null, null)
                .stream().toList();

        if (messages.isEmpty()) return "No response received.";
        ThreadMessage latest = messages.get(0);
        StringBuilder sb = new StringBuilder();
        for (MessageContent content : latest.getContent()) {
            if (content instanceof MessageTextContent textContent) {
                sb.append(textContent.getText().getValue());
            }
        }
        return sb.isEmpty() ? "No response received." : sb.toString();
    }
}
