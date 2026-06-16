package com.innowise.orderservice.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.orderservice.dto.OrderItemDto;
import com.innowise.orderservice.dto.OrderRequest;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.dao.ItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long itemId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE order_items, orders, items RESTART IDENTITY CASCADE");

        Item item = new Item();
        item.setName("Keyboard");
        item.setPrice(BigDecimal.valueOf(50));
        itemId = itemRepository.save(item).getId();

        stubUserService();
    }

    private void stubUserService() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/users/email/anna%40example.com"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 10,
                                  "name": "Anna",
                                  "surname": "Ivanova",
                                  "email": "anna@example.com",
                                  "active": true
                                }
                                """)));
    }

    private OrderRequest buildOrderRequest() {
        OrderItemDto orderItemDto = new OrderItemDto(null, itemId, 2);
        return new OrderRequest(10L, "anna@example.com", OrderStatus.NEW, List.of(orderItemDto));
    }

    @Test
    void createOrder_shouldPersistAndReturnEnrichedOrder() throws Exception {
        String body = objectMapper.writeValueAsString(buildOrderRequest());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(10)))
                .andExpect(jsonPath("$.totalPrice", is(100.0)))
                .andExpect(jsonPath("$.user.email", is("anna@example.com")))
                .andExpect(jsonPath("$.status", is("NEW")));
    }

    @Test
    void getOrderById_shouldReturnOrderWithUserInfo() throws Exception {
        String body = objectMapper.writeValueAsString(buildOrderRequest());
        String response = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.user.name", is("Anna")));
    }

    @Test
    void getOrderById_shouldReturnNotFoundForMissingOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Order Not Found")));
    }

    @Test
    void createOrder_shouldReturnBadRequestForInvalidPayload() throws Exception {
        OrderRequest invalid = new OrderRequest(null, null, null, List.of());
        String body = objectMapper.writeValueAsString(invalid);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Error")));
    }

    @Test
    void updateOrder_shouldUpdateAndReturnOrder() throws Exception {
        String createBody = objectMapper.writeValueAsString(buildOrderRequest());
        String createResponse = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(createResponse).get("id").asLong();

        OrderItemDto updatedItem = new OrderItemDto(null, itemId, 5);
        OrderRequest updateRequest = new OrderRequest(10L, "anna@example.com",
                OrderStatus.PROCESSING, List.of(updatedItem));
        String updateBody = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/api/v1/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSING")))
                .andExpect(jsonPath("$.totalPrice", is(250.0)));
    }

    @Test
    void getOrders_shouldFilterByStatus() throws Exception {
        String body = objectMapper.writeValueAsString(buildOrderRequest());
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders").param("statuses", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("NEW")));

        mockMvc.perform(get("/api/v1/orders").param("statuses", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getOrders_shouldFilterByUserId() throws Exception {
        String body = objectMapper.writeValueAsString(buildOrderRequest());
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders").param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId", is(10)));

        mockMvc.perform(get("/api/v1/orders").param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getOrders_shouldFilterByDateRange() throws Exception {
        String body = objectMapper.writeValueAsString(buildOrderRequest());
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders")
                        .param("createdFrom", "2020-01-01T00:00:00")
                        .param("createdTo", "2030-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void deleteOrder_shouldSoftDeleteAndExcludeFromQueries() throws Exception {
        String body = objectMapper.writeValueAsString(buildOrderRequest());
        String response = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_shouldFallbackWhenUserServiceUnavailable() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/users/email/anna%40example.com"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        String body = objectMapper.writeValueAsString(buildOrderRequest());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(10)))
                .andExpect(jsonPath("$.user.email", is("anna@example.com")));
    }
}