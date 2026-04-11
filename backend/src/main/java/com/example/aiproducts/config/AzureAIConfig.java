package com.example.aiproducts.config;

import com.azure.ai.agents.persistent.PersistentAgentsClient;
import com.azure.ai.agents.persistent.PersistentAgentsClientBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
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
     * Azure AI Agents Persistent client — manages agents, threads, messages, and runs.
     * The endpoint is the Azure AI Foundry project endpoint.
     */
    @Bean
    public PersistentAgentsClient persistentAgentsClient(DefaultAzureCredential credential) {
        return new PersistentAgentsClientBuilder()
                .endpoint(projectEndpoint)
                .credential(credential)
                .buildClient();
    }

    /**
     * Azure AI Search client for direct product searches outside of agent context.
     * Uses DefaultAzureCredential (RBAC) — no API key required when using managed identity.
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
