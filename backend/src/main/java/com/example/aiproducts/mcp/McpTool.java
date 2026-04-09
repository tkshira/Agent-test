package com.example.aiproducts.mcp;

import java.util.Map;

/**
 * Contract for MCP tool implementations.
 *
 * Each tool must:
 * 1. Declare its name (used for routing in McpServer)
 * 2. Provide an input schema (JSON Schema object) for tools/list discovery
 * 3. Execute when called with the provided arguments
 */
public interface McpTool {

    /** Unique tool name as it appears in the MCP tools/list response. */
    String getName();

    /** Human-readable description exposed to clients/agents. */
    String getDescription();

    /**
     * JSON Schema describing the tool's input parameters.
     * Returned as-is in the tools/list response so agents know how to call it.
     */
    Map<String, Object> getInputSchema();

    /**
     * Execute the tool.
     *
     * @param arguments key-value arguments matching the declared input schema
     * @return tool result (serialized as JSON in the MCP response)
     */
    Object execute(Map<String, Object> arguments);
}
