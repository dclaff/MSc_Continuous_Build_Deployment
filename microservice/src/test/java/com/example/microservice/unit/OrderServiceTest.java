package com.example.microservice.unit;

import com.example.microservice.dto.CreateOrderRequest;
import com.example.microservice.dto.OrderDTO;
import com.example.microservice.dto.PagedResponse;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.model.Customer;
import com.example.microservice.model.Order;
import com.example.microservice.repository.CustomerRepository;
import com.example.microservice.repository.OrderRepository;
import com.example.microservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for OrderService.
 * No Spring context is loaded. All collaborators are replaced with Mockito mocks,
 * keeping tests fast and isolated from I/O.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private CustomerRepository customerRepo;

    @InjectMocks
    private OrderService orderService;

    private static final Customer ALICE =
            new Customer(1L, "Alice Johnson", "alice@example.com", "555-0001",
                    LocalDate.of(2025, 1, 15));

    // -------------------------------------------------------------------------
    // getOrdersByCustomer
    // -------------------------------------------------------------------------

    @Test
    void getOrdersByCustomer_returnsMappedPagedResponse() {
        Order order = new Order(10L, 1L, "Widget", new BigDecimal("49.99"),
                LocalDate.of(2025, 6, 1), "PENDING");

        when(customerRepo.findById(1L)).thenReturn(Optional.of(ALICE));
        when(orderRepo.findByCustomerId(1L, 0, 10)).thenReturn(List.of(order));
        when(orderRepo.countByCustomerId(1L)).thenReturn(1L);

        PagedResponse<OrderDTO> result = orderService.getOrdersByCustomer(1L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Widget");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void getOrdersByCustomer_throwsWhenCustomerDoesNotExist() {
        when(customerRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrdersByCustomer(99L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with id: 99");
    }

    // -------------------------------------------------------------------------
    // createOrder
    // -------------------------------------------------------------------------

    @Test
    void createOrder_defaultsStatusToPending_whenNotProvided() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setDescription("Gadget");
        req.setAmount(new BigDecimal("99.50"));
        req.setOrderDate(LocalDate.of(2025, 9, 1));
        // status intentionally left null

        Order saved = new Order(20L, 1L, "Gadget", new BigDecimal("99.50"),
                LocalDate.of(2025, 9, 1), "PENDING");

        when(customerRepo.findById(1L)).thenReturn(Optional.of(ALICE));
        when(orderRepo.save(any(Order.class))).thenReturn(saved);

        OrderDTO result = orderService.createOrder(1L, req);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(orderRepo).save(any(Order.class));
    }

    @Test
    void createOrder_usesProvidedStatus_whenSupplied() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setDescription("Gadget");
        req.setAmount(new BigDecimal("99.50"));
        req.setOrderDate(LocalDate.of(2025, 9, 1));
        req.setStatus("SHIPPED");

        Order saved = new Order(21L, 1L, "Gadget", new BigDecimal("99.50"),
                LocalDate.of(2025, 9, 1), "SHIPPED");

        when(customerRepo.findById(1L)).thenReturn(Optional.of(ALICE));
        when(orderRepo.save(any(Order.class))).thenReturn(saved);

        OrderDTO result = orderService.createOrder(1L, req);

        assertThat(result.getStatus()).isEqualTo("SHIPPED");
    }

    // -------------------------------------------------------------------------
    // getOrder
    // -------------------------------------------------------------------------

    @Test
    void getOrder_throwsWhenOrderNotFound() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(ALICE));
        when(orderRepo.findByIdAndCustomerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 99");
    }
}
