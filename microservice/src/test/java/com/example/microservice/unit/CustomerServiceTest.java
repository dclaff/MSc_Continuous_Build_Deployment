package com.example.microservice.unit;

import com.example.microservice.dto.CreateCustomerRequest;
import com.example.microservice.dto.CustomerDTO;
import com.example.microservice.dto.CustomerDetailDTO;
import com.example.microservice.dto.PagedResponse;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.model.Customer;
import com.example.microservice.repository.CustomerRepository;
import com.example.microservice.repository.OrderRepository;
import com.example.microservice.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for CustomerService.
 * No Spring context is loaded. All collaborators are replaced with Mockito mocks
 * injected via constructor, reflecting the constructor-injection pattern used in
 * the production class.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepo;

    @Mock
    private OrderRepository orderRepo;

    @InjectMocks
    private CustomerService customerService;

    // -------------------------------------------------------------------------
    // getAllCustomers
    // -------------------------------------------------------------------------

    @Test
    void getAllCustomers_mapsCustomersToDTOsWithOrderCount() {
        Customer alice = new Customer(1L, "Alice Johnson", "alice@example.com", "555-0001",
                LocalDate.of(2025, 1, 15));

        when(customerRepo.findAll(0, 10)).thenReturn(List.of(alice));
        when(customerRepo.count()).thenReturn(1L);
        when(customerRepo.countOrdersByCustomerId(1L)).thenReturn(3);

        PagedResponse<CustomerDTO> result = customerService.getAllCustomers(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Alice Johnson");
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getContent().get(0).getTotalOrders()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    void getAllCustomers_emptyList_returnsEmptyPage() {
        when(customerRepo.findAll(0, 10)).thenReturn(List.of());
        when(customerRepo.count()).thenReturn(0L);

        PagedResponse<CustomerDTO> result = customerService.getAllCustomers(0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // -------------------------------------------------------------------------
    // getCustomerById
    // -------------------------------------------------------------------------

    @Test
    void getCustomerById_returnsDetailDTOWithEmptyOrders() {
        Customer alice = new Customer(1L, "Alice Johnson", "alice@example.com", "555-0001",
                LocalDate.of(2025, 1, 15));

        when(customerRepo.findById(1L)).thenReturn(Optional.of(alice));
        when(orderRepo.findByCustomerId(1L, 0, Integer.MAX_VALUE)).thenReturn(List.of());

        CustomerDetailDTO result = customerService.getCustomerById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Alice Johnson");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getCreatedAt()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(result.getOrders()).isEmpty();
    }

    @Test
    void getCustomerById_throwsResourceNotFoundException_whenNotFound() {
        when(customerRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // -------------------------------------------------------------------------
    // createCustomer
    // -------------------------------------------------------------------------

    @Test
    void createCustomer_persistsEntityAndReturnsMappedDTO() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setName("Dan Test");
        req.setEmail("dan@test.com");
        req.setPhone("555-9999");

        Customer saved = new Customer(42L, "Dan Test", "dan@test.com", "555-9999", LocalDate.now());

        when(customerRepo.save(any(Customer.class))).thenReturn(saved);

        CustomerDetailDTO result = customerService.createCustomer(req);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getName()).isEqualTo("Dan Test");
        assertThat(result.getEmail()).isEqualTo("dan@test.com");
        assertThat(result.getOrders()).isEmpty();
        verify(customerRepo).save(any(Customer.class));
    }
}
