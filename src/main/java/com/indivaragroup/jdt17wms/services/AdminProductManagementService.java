package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.response.ProductResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AdminProductManagementService {

    private final ProductRepository productRepository;

    public AdminProductManagementService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponseDTO> listProducts(String search, String type, Pageable pageable) {
        String s = (search != null && search.trim().isEmpty()) ? null : search;
        String t = (type != null && type.trim().isEmpty()) ? null : type;
        return productRepository.findAllAdmin(s, t, pageable).map(ProductResponseDTO::fromEntity);
    }
    @Transactional
    public ProductResponseDTO updateProductVisibility(UUID id, Boolean visibility) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));
        product.setVisible(visibility);
        return ProductResponseDTO.fromEntity(productRepository.save(product));
    }
}
