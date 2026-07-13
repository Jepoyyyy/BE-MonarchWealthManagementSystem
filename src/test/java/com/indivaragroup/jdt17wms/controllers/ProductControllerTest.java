package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.services.ProductManagementService;
import com.indivaragroup.jdt17wms.services.JwtService;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductManagementService productManagementService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getAllProducts_shouldReturnOk() throws Exception {
        Page<Product> expectedPage = new PageImpl<>(List.of());
        when(productManagementService.getAllProducts(any(Pageable.class))).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProduct_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        when(productManagementService.updateProductVisibility(any(UUID.class), any(Boolean.class)))
                .thenReturn(product);

        mockMvc.perform(put("/api/v1/products/" + id)
                        .contentType("application/json")
                        .content("{\"visibility\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProduct_shouldReturnNotFound_whenProductDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(productManagementService.updateProductVisibility(any(UUID.class), any(Boolean.class)))
                .thenThrow(new com.indivaragroup.jdt17wms.exceptions.NotFoundException("No valid item with the ID"));

        mockMvc.perform(put("/api/v1/products/" + id)
                        .contentType("application/json")
                        .content("{\"visibility\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error").value("No valid item with the ID"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(404));
    }
}


