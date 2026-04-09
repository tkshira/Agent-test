package com.example.aiproducts.controller;

import com.example.aiproducts.model.ChatRequest;
import com.example.aiproducts.model.ChatResponse;
import com.example.aiproducts.service.AgentService;
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

    /**
     * POST /api/chat
     *
     * Send a message to the Azure AI Foundry agent.
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
