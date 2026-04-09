package com.example.aiproducts.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP JSON-RPC 2.0 response envelope.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {

    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonrpc = "2.0";

    private Object id;

    /** Present on success. */
    private Object result;

    /** Present on error. */
    private McpError error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpError {
        private int code;
        private String message;
        private Object data;
    }

    // --- Factory helpers ---

    public static McpResponse success(Object id, Object result) {
        return McpResponse.builder().id(id).result(result).build();
    }

    public static McpResponse error(Object id, int code, String message) {
        return McpResponse.builder()
                .id(id)
                .error(McpError.builder().code(code).message(message).build())
                .build();
    }
}
