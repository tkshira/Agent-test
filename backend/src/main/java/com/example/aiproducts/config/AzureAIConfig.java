package com.example.aiproducts.config;

import com.azure.ai.agents.persistent.PersistentAgentsClient;
import com.azure.ai.agents.persistent.PersistentAgentsClientBuilder;
import com.azure.ai.projects.AIProjectClientBuilder;
import com.azure.ai.projects.ConnectionsClient;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.models.SemanticSearchOptions;
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

    @Value("${azure.ai.search.semantic-configuration-name:products-indexer-semantic}")
    private String searchSemanticConfigurationName;

    public SemanticSearchOptions getSemanticSearchOptions() {
        return new SemanticSearchOptions()
                .setSemanticConfigurationName(searchSemanticConfigurationName);
    }

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
     * Azure AI Projects client builder — shared base for all project-scoped clients.
     */
    @Bean
    public AIProjectClientBuilder aiProjectClientBuilder(DefaultAzureCredential credential) {
        return new AIProjectClientBuilder()
                .endpoint(projectEndpoint)
                .credential(credential);
    }

    /**
     * Connections client for resolving registered project connections (e.g. Azure AI Search).
     * Used to look up the full ARM connection ID from a connection name.
     */
    @Bean
    public ConnectionsClient connectionsClient(AIProjectClientBuilder builder) {
        return builder.buildConnectionsClient();
    }

    /**
     * Persistent Agents client for managing agents, threads, and runs.
     * Uses the same endpoint and credential as the AI Project client.
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
