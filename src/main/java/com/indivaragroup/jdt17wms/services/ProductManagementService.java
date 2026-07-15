package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductManagementService {

    private final ProductRepository productRepository;

    public ProductManagementService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product updateProductVisibility(UUID id, Boolean visibility) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.NOT_FOUND,"No valid item with the ID"));
        product.setVisible(visibility);
        return productRepository.save(product);
    }
}


