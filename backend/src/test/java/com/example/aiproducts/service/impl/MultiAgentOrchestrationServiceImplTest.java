package com.example.aiproducts.service.impl;

import com.azure.ai.agents.persistent.PersistentAgentsClient;
import com.azure.ai.agents.persistent.ThreadsClient;
import com.azure.ai.agents.persistent.models.PersistentAgentThread;
import com.example.aiproducts.model.AgentRole;
import com.example.aiproducts.model.ChatResponse;
import com.example.aiproducts.model.Product;
import com.example.aiproducts.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MultiAgentOrchestrationServiceImplTest {

    @Mock
    private PersistentAgentsClient agentsClient;
    @Mock
    private SearchService searchService;
    @Mock
    private ThreadsClient threadsClient;
    @Mock
    private PersistentAgentThread mockThread;

    @InjectMocks
    private MultiAgentOrchestrationServiceImpl service;

    @BeforeEach
    void setUp() {
        // Inject @Value fields — Spring doesn't process these in plain unit tests
        ReflectionTestUtils.setField(service, "model", "gpt-4o");
        ReflectionTestUtils.setField(service, "productExpertName", "ProductExpertAgent");
        ReflectionTestUtils.setField(service, "purchaseAdvisorName", "PurchaseAdvisorAgent");
        ReflectionTestUtils.setField(service, "supportName", "SupportAgent");
        ReflectionTestUtils.setField(service, "productExpertInstructions", "Product expert instructions");
        ReflectionTestUtils.setField(service, "purchaseAdvisorInstructions", "Purchase advisor instructions");
        ReflectionTestUtils.setField(service, "supportInstructions", "Support instructions");
        ReflectionTestUtils.setField(service, "searchConnectionId", "mock-connection-id");
        ReflectionTestUtils.setField(service, "searchIndexName", "products");

        // Supply pre-configured IDs so initAgents() skips Azure API calls
        ReflectionTestUtils.setField(service, "configuredProductExpertId", "agent-product-expert");
        ReflectionTestUtils.setField(service, "configuredPurchaseAdvisorId", "agent-purchase-advisor");
        ReflectionTestUtils.setField(service, "configuredSupportId", "agent-support");

        service.initAgents();

        // Thread mocks — only orchestrate() tests use these; lenient avoids false UnnecessaryStubbing errors
        lenient().when(agentsClient.getThreadsClient()).thenReturn(threadsClient);
        lenient().when(threadsClient.createThread()).thenReturn(mockThread);
        lenient().when(mockThread.getId()).thenReturn("thread-123");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // route() – keyword classification
    // ─────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "''{0}'' → {1}")
    @CsvSource({
        "I want to buy headphones,             PURCHASE_ADVISOR",
        "What is the price of this laptop?,    PURCHASE_ADVISOR",
        "I need to purchase a keyboard,        PURCHASE_ADVISOR",
        "How much does it cost?,               PURCHASE_ADVISOR",
        "I have a problem with my order,       SUPPORT",
        "My product is broken,                 SUPPORT",
        "I want to return this item,           SUPPORT",
        "I need help with my refund,           SUPPORT",
        "Tell me about your headphones,        PRODUCT_EXPERT",
        "What products do you sell?,           PRODUCT_EXPERT",
        "Show me your laptop collection,       PRODUCT_EXPERT"
    })
    void route_shouldReturnCorrectRole(String message, AgentRole expectedRole) {
        assertThat(service.route(message)).isEqualTo(expectedRole);
    }

    @Test
    void route_shouldBeCaseInsensitive() {
        assertThat(service.route("RETURN MY PRODUCT")).isEqualTo(AgentRole.SUPPORT);
        assertThat(service.route("BUY NOW")).isEqualTo(AgentRole.PURCHASE_ADVISOR);
    }

    @Test
    void route_emptyMessage_defaultsToProductExpert() {
        assertThat(service.route("")).isEqualTo(AgentRole.PRODUCT_EXPERT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // orchestrate() – end-to-end routing and response shape
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void orchestrate_purchaseMessage_routesToPurchaseAdvisor() {
        ChatResponse response = service.orchestrate("I want to buy headphones", null);

        assertThat(response.getHandledBy()).isEqualTo(AgentRole.PURCHASE_ADVISOR);
        assertThat(response.getThreadId()).isEqualTo("thread-123");
        assertThat(response.getMessage()).contains("PurchaseAdvisor");
        assertThat(response.getProducts()).isEmpty();
        assertThat(response.isError()).isFalse();
    }

    @Test
    void orchestrate_supportMessage_routesToSupport() {
        ChatResponse response = service.orchestrate("I have a problem with my order", null);

        assertThat(response.getHandledBy()).isEqualTo(AgentRole.SUPPORT);
        assertThat(response.getThreadId()).isEqualTo("thread-123");
        assertThat(response.getMessage()).contains("Support");
        assertThat(response.getProducts()).isEmpty();
    }

    @Test
    void orchestrate_productMessage_routesToProductExpert_andIncludesProducts() {
        Product product = Product.builder()
                .id("p1")
                .name("Noise-Cancelling Headphones")
                .price(new BigDecimal("199.99"))
                .build();
        when(searchService.searchProducts(anyString(), anyInt())).thenReturn(List.of(product));

        ChatResponse response = service.orchestrate("Tell me about headphones", null);

        assertThat(response.getHandledBy()).isEqualTo(AgentRole.PRODUCT_EXPERT);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getName()).isEqualTo("Noise-Cancelling Headphones");
        assertThat(response.getMessage()).contains("ProductExpert");
    }

    @Test
    void orchestrate_nullThread_createsNewThread() {
        service.orchestrate("Show me keyboards", null);

        verify(threadsClient).createThread();
        verify(threadsClient, never()).getThread(anyString());
    }

    @Test
    void orchestrate_existingThread_reusesThread() {
        when(threadsClient.getThread("existing-thread")).thenReturn(mockThread);
        when(mockThread.getId()).thenReturn("existing-thread");

        ChatResponse response = service.orchestrate("Show me keyboards", "existing-thread");

        assertThat(response.getThreadId()).isEqualTo("existing-thread");
        verify(threadsClient, never()).createThread();
    }

    @Test
    void orchestrate_searchFailure_returnsProductExpertResponseWithEmptyProducts() {
        when(searchService.searchProducts(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Search unavailable"));

        ChatResponse response = service.orchestrate("Tell me about headphones", null);

        assertThat(response.getHandledBy()).isEqualTo(AgentRole.PRODUCT_EXPERT);
        assertThat(response.getProducts()).isEmpty();
        assertThat(response.isError()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initAgents() – agent setup
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initAgents_withPreConfiguredIds_doesNotCallAzureAdminApi() {
        verify(agentsClient, never()).getPersistentAgentsAdministrationClient();
    }
}
