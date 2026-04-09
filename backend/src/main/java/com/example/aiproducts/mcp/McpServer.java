package com.example.aiproducts.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Embedded MCP (Model Context Protocol) server.
 *
 * Implements the core JSON-RPC 2.0 methods from the MCP spec:
 * - initialize      → server capability negotiation
 * - tools/list      → enumerate registered tools
 * - tools/call      → invoke a specific tool by name
 *
 * Tools are auto-registered via Spring's dependency injection.
 * To add a new tool: implement McpTool, annotate with @Component.
 *
 * Transport is handled by McpController (HTTP POST /mcp/v1).
 * Swap to SSE or stdio transport by changing only the controller.
 */
@Slf4j
@Component
public class McpServer {

    @Value("${mcp.server.name:ProductChatbotMCP}")
    private String serverName;

    @Value("${mcp.server.version:1.0.0}")
    private String serverVersion;

    private final Map<String, McpTool> tools;

    public McpServer(List<McpTool> registeredTools) {
        this.tools = registeredTools.stream()
                .collect(Collectors.toMap(McpTool::getName, t -> t));
        log.info("MCP Server initialized with tools: {}", tools.keySet());
    }

    /**
     * Handle an incoming MCP JSON-RPC request and return the response.
     */
    public McpResponse handle(McpRequest request) {
        if (request.getMethod() == null) {
            return McpResponse.error(request.getId(), -32600, "Invalid request: missing method");
        }

        return switch (request.getMethod()) {
            case "initialize"  -> handleInitialize(request);
            case "tools/list"  -> handleToolsList(request);
            case "tools/call"  -> handleToolsCall(request);
            default            -> McpResponse.error(request.getId(), -32601,
                    "Method not found: " + request.getMethod());
        };
    }

    /**
     * Directly invoke a tool by name (used internally by PurchaseServiceImpl).
     */
    public Object callTool(String toolName, Map<String, Object> arguments) {
        McpTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("MCP tool not registered: " + toolName);
        }
        return tool.execute(arguments);
    }

    // --- Private handlers ---

    private McpResponse handleInitialize(McpRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", Map.of("name", serverName, "version", serverVersion));
        result.put("capabilities", Map.of("tools", Map.of()));
        return McpResponse.success(request.getId(), result);
    }

    private McpResponse handleToolsList(McpRequest request) {
        List<Map<String, Object>> toolList = tools.values().stream()
                .map(tool -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("name",        tool.getName());
                    entry.put("description", tool.getDescription());
                    entry.put("inputSchema", tool.getInputSchema());
                    return entry;
                })
                .collect(Collectors.toList());

        return McpResponse.success(request.getId(), Map.of("tools", toolList));
    }

    @SuppressWarnings("unchecked")
    private McpResponse handleToolsCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            return McpResponse.error(request.getId(), -32602, "Missing params");
        }

        String toolName = (String) params.get("name");
        if (toolName == null || toolName.isBlank()) {
            return McpResponse.error(request.getId(), -32602, "Missing tool name");
        }

        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return McpResponse.error(request.getId(), -32601, "Tool not found: " + toolName);
        }

        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        try {
            Object result = tool.execute(arguments);
            return McpResponse.success(request.getId(), Map.of(
                    "content", List.of(Map.of("type", "text", "text", result.toString())),
                    "isError", false
            ));
        } catch (IllegalArgumentException e) {
            return McpResponse.error(request.getId(), -32602, "Invalid arguments: " + e.getMessage());
        } catch (Exception e) {
            log.error("Tool execution failed: tool={} error={}", toolName, e.getMessage(), e);
            return McpResponse.error(request.getId(), -32603, "Tool execution error: " + e.getMessage());
        }
    }
}
