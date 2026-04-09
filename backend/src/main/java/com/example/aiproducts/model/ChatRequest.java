package com.example.aiproducts.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    /**
     * The user's message text.
     */
    @NotBlank(message = "Message must not be blank")
    private String message;

    /**
     * Thread/session ID. If null or blank, a new thread is created.
     * The client should persist and re-send this to maintain conversation context.
     */
    private String threadId;
}
