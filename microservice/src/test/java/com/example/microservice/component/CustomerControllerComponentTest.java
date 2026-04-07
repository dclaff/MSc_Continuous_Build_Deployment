package com.example.microservice.component;

import com.example.microservice.controller.CustomerController;
import com.example.microservice.dto.CustomerDetailDTO;
import com.example.microservice.dto.CustomerDTO;
import com.example.microservice.dto.PagedResponse;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component-level (slice) tests for CustomerController.
 *
 * Uses @WebMvcTest to load only the web layer (controller, filters, Jackson,
 * validation) without starting a full application context or touching the
 * database. CustomerService is replaced with a @MockBean so these tests
 * exercise HTTP serialisation, routing, and bean-validation in isolation.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerComponentTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    // -------------------------------------------------------------------------
    // GET /api/customers
    // -------------------------------------------------------------------------

    @Test
    void listCustomers_returns200WithPagedContent() throws Exception {
        CustomerDTO dto = new CustomerDTO(1L, "Alice Johnson", "alice@example.com", 2);
        PagedResponse<CustomerDTO> page = new PagedResponse<>(List.of(dto), 0, 10, 1L);

        when(customerService.getAllCustomers(0, 10)).thenReturn(page);

        mvc.perform(get("/api/customers?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alice Johnson"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    // -------------------------------------------------------------------------
    // GET /api/customers/{id}
    // -------------------------------------------------------------------------

    @Test
    void getCustomer_returns200WithDetailDTO() throws Exception {
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setId(1L);
        detail.setName("Alice Johnson");
        detail.setEmail("alice@example.com");
        detail.setCreatedAt(LocalDate.of(2025, 1, 15));
        detail.setOrders(List.of());

        when(customerService.getCustomerById(1L)).thenReturn(detail);

        mvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.orders").isArray());
    }

    @Test
    void getCustomer_returns404_whenServiceThrowsNotFound() throws Exception {
        when(customerService.getCustomerById(99L))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 99"));

        mvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99"));
    }

    // -------------------------------------------------------------------------
    // POST /api/customers — Bean Validation (no service call needed)
    // -------------------------------------------------------------------------

    @Test
    void createCustomer_missingName_returns400WithValidationError() throws Exception {
        String body = """
                { "email": "valid@example.com" }
                """;

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createCustomer_malformedEmail_returns400() throws Exception {
        String body = """
                { "name": "Test User", "email": "not-an-email" }
                """;

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    void createCustomer_validRequest_returns201() throws Exception {
        CustomerDetailDTO created = new CustomerDetailDTO();
        created.setId(42L);
        created.setName("Bob New");
        created.setEmail("bob@new.com");
        created.setOrders(List.of());

        when(customerService.createCustomer(any())).thenReturn(created);

        String body = """
                { "name": "Bob New", "email": "bob@new.com" }
                """;

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Bob New"));
    }
}
