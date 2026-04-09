package com.example.aiproducts.controller;

import com.example.aiproducts.mcp.McpRequest;
import com.example.aiproducts.mcp.McpResponse;
import com.example.aiproducts.mcp.McpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP transport endpoint for the embedded MCP server.
 *
 * Exposes MCP over HTTP POST following the JSON-RPC 2.0 protocol.
 * External agents or clients can discover and call tools via:
 *
 *   POST /mcp/v1
 *   Content-Type: application/json
 *
 *   { "jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {} }
 *
 * To switch transport (e.g., SSE or stdio), replace this controller
 * while keeping McpServer unchanged.
 */
@Slf4j
@RestController
@RequestMapping("/mcp/v1")
@RequiredArgsConstructor
public class McpController {

    private final McpServer mcpServer;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public McpResponse handle(@RequestBody McpRequest request) {
        log.debug("MCP request: method={} id={}", request.getMethod(), request.getId());
        McpResponse response = mcpServer.handle(request);
        log.debug("MCP response: id={} hasError={}", response.getId(), response.getError() != null);
        return response;
    }
}
