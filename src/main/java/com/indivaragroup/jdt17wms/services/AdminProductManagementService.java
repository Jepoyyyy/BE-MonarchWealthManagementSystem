package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.dto.request.AdminProductCreateDTO;
import com.indivaragroup.jdt17wms.dto.request.AdminProductUpdateDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminProductManagementService {

    private final ProductRepository productRepository;

    public AdminProductManagementService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> listProducts(String search, String type, Pageable pageable) {
        String s = (search != null && search.trim().isEmpty()) ? null : search;
        String t = (type != null && type.trim().isEmpty()) ? null : type;
        return productRepository.findAllAdmin(s, t, pageable);
    }

    public Product createProduct(AdminProductCreateDTO dto) {
        Product p = Product.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .issuer(dto.getIssuer())
                .type(dto.getType())
                .riskLevel(dto.getRiskLevel())
                .annualReturn(dto.getAnnualReturn())
                .minInvestment(dto.getMinInvestment())
                .currentPrice(dto.getCurrentPrice())
                .description(dto.getDescription())
                .tenor(dto.getTenor())
                .lotSize(dto.getLotSize())
                .isFractionalAllowed(dto.getIsFractionalAllowed())
                .visible(dto.getVisible())
                .build();
        return productRepository.save(p);
    }

    public Product updateProduct(UUID id, AdminProductUpdateDTO dto) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND, "Product not found"));
        if (dto.getName() != null) p.setName(dto.getName());
        if (dto.getIssuer() != null) p.setIssuer(dto.getIssuer());
        if (dto.getType() != null) p.setType(dto.getType());
        if (dto.getRiskLevel() != null) p.setRiskLevel(dto.getRiskLevel());
        if (dto.getAnnualReturn() != null) p.setAnnualReturn(dto.getAnnualReturn());
        if (dto.getMinInvestment() != null) p.setMinInvestment(dto.getMinInvestment());
        if (dto.getCurrentPrice() != null) p.setCurrentPrice(dto.getCurrentPrice());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getTenor() != null) p.setTenor(dto.getTenor());
        if (dto.getLotSize() != null) p.setLotSize(dto.getLotSize());
        if (dto.getIsFractionalAllowed() != null) p.setIsFractionalAllowed(dto.getIsFractionalAllowed());
        if (dto.getVisible() != null) p.setVisible(dto.getVisible());
        return productRepository.save(p);
    }
}
