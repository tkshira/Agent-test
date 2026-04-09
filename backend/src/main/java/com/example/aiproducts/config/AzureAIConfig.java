package com.example.aiproducts.config;

import com.azure.ai.projects.AIProjectClient;
import com.azure.ai.projects.AIProjectClientBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureAIConfig {

    @Value("${azure.ai.project-endpoint}")
    private String projectEndpoint;

    @Value("${azure.ai.search.endpoint}")
    private String searchEndpoint;

    @Value("${azure.ai.search.index-name}")
    private String searchIndexName;

    /**
     * DefaultAzureCredential supports:
     * - Environment variables (AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID)
     * - Azure CLI (az login)
     * - Managed Identity (when deployed to Azure)
     * - Visual Studio Code credential
     */
    @Bean
    public DefaultAzureCredential defaultAzureCredential() {
        return new DefaultAzureCredentialBuilder().build();
    }

    /**
     * Azure AI Projects client — entry point to Azure AI Foundry agents, threads, etc.
     * Swap the endpoint to connect to a different Foundry project.
     */
    @Bean
    public AIProjectClient aiProjectClient(DefaultAzureCredential credential) {
        return new AIProjectClientBuilder()
                .endpoint(projectEndpoint)
                .credential(credential)
                .buildClient();
    }

    /**
     * Azure AI Search client for direct product searches outside of agent context.
     * Uses DefaultAzureCredential (RBAC) — no API key required when using managed identity.
     *
     * To use an API key instead, replace with:
     *   .credential(new AzureKeyCredential(apiKey))
     */
    @Bean
    public SearchClient searchClient(DefaultAzureCredential credential) {
        return new SearchClientBuilder()
                .endpoint(searchEndpoint)
                .indexName(searchIndexName)
                .credential(credential)
                .buildClient();
    }
}
