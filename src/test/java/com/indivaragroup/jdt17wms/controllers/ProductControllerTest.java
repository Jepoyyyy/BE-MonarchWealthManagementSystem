package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.services.ProductManagementService;
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

import com.indivaragroup.jdt17wms.dto.request.ProductQueryDTO;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductManagementService productManagementService;



    @Test
    void getAllProducts_shouldReturnOk() throws Exception {
        Page<ProductResponseDTO> expectedPage = new PageImpl<>(List.of());
        when(productManagementService.getProductsForUser(any(ProductQueryDTO.class), any(Pageable.class))).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProduct_shouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponseDTO product = ProductResponseDTO.builder().build();
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
                .thenThrow(new CoreThrowHandler(ApiError.NOT_FOUND, "No valid item with the ID"));

        mockMvc.perform(put("/api/v1/products/" + id)
                        .contentType("application/json")
                        .content("{\"visibility\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("No valid item with the ID"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(404));
    }
}


