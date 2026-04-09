package com.example.aiproducts.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * MCP JSON-RPC 2.0 request envelope.
 * Spec: https://spec.modelcontextprotocol.io/specification/
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpRequest {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    /** Request ID (string or number); null for notifications. */
    private Object id;

    /**
     * MCP method name.
     * Supported: "initialize", "tools/list", "tools/call"
     */
    private String method;

    /** Method parameters — structure varies per method. */
    private Map<String, Object> params;
}
