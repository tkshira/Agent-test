package com.example.aiproducts.controller;

import com.example.aiproducts.model.ChatRequest;
import com.example.aiproducts.model.ChatResponse;
import com.example.aiproducts.service.AgentService;
import com.example.aiproducts.service.MultiAgentOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AgentService agentService;
    private final MultiAgentOrchestrationService multiAgentOrchestrationService;

    /**
     * POST /api/chat
     *
     * Send a message to a single Azure AI Foundry agent.
     * Pass threadId from the previous response to continue the conversation.
     *
     * Request:  { "message": "Tell me about your headphones", "threadId": null }
     * Response: { "message": "...", "products": [...], "threadId": "thread_abc123" }
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.debug("Chat request: thread={} message={}", request.getThreadId(), request.getMessage());
        ChatResponse response = agentService.chat(request.getMessage(), request.getThreadId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/chat/multi
     *
     * Send a message through the multi-agent orchestration pipeline.
     * A keyword router classifies intent and delegates to the best-fit specialist:
     *   - ProductExpert    – product catalog queries
     *   - PurchaseAdvisor  – buying decisions and order flow
     *   - Support          – issues, returns, and complaints
     *
     * The response includes a {@code handledBy} field indicating which agent responded.
     *
     * Request:  { "message": "I want to buy headphones", "threadId": null }
     * Response: { "message": "...", "handledBy": "PURCHASE_ADVISOR", "threadId": "thread_abc123" }
     */
    @PostMapping("/multi")
    public ResponseEntity<ChatResponse> multiAgentChat(@Valid @RequestBody ChatRequest request) {
        log.debug("Multi-agent request: thread={} message={}", request.getThreadId(), request.getMessage());
        ChatResponse response = multiAgentOrchestrationService.orchestrate(
                request.getMessage(), request.getThreadId());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/chat/{threadId}
     *
     * Clean up a conversation thread when the session ends.
     */
    @DeleteMapping("/{threadId}")
    public ResponseEntity<Void> deleteThread(@PathVariable String threadId) {
        agentService.deleteThread(threadId);
        return ResponseEntity.noContent().build();
    }
}
