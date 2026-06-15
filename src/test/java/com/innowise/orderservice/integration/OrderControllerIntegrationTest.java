package com.innowise.orderservice.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dto.OrderItemDto;
import com.innowise.orderservice.dto.OrderRequest;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Long itemId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();

        Item item = new Item();
        item.setName("Keyboard");
        item.setPrice(BigDecimal.valueOf(50));
        itemId = itemRepository.save(item).getId();

        stubUserService();
    }

    private void stubUserService() {
        stubFor(get(urlEqualTo("/api/v1/users/10"))
                .willReturn(aResponse()
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
        return new OrderRequest(10L, OrderStatus.NEW, List.of(orderItemDto));
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
        OrderRequest invalid = new OrderRequest(null, null, List.of());
        String body = objectMapper.writeValueAsString(invalid);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Error")));
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
        stubFor(get(urlEqualTo("/api/v1/users/10"))
                .willReturn(aResponse().withStatus(500)));

        String body = objectMapper.writeValueAsString(buildOrderRequest());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(10)))
                .andExpect(jsonPath("$.user.id", is(10)));
    }
}