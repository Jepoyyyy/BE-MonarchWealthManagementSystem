package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.constants.ProductConstants;
import com.indivaragroup.jdt17wms.constants.RiskConstants;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.aspects.RiskProfileAssessmentRequired;
import com.indivaragroup.jdt17wms.dto.request.ProductQueryDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.Product;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProductManagementService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductManagementService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private boolean isNonAdminUser(User user) {
        return user != null && user.getRole() != UserRole.ADMIN;
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @RiskProfileAssessmentRequired
    public Page<Product> getProductsForUser(
            ProductQueryDTO queryDTO,
            Pageable pageable) {

        ProductQueryDTO dto = queryDTO != null ? queryDTO : new ProductQueryDTO();
        String searchQuery = dto.getSearchQuery();
        String type = dto.getType();
        Boolean showAll = dto.getShowAll();
        Boolean dashboardSummary = dto.getDashboardSummary();

        User user = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);

        List<Product> products = productRepository.findAll();

        // 1. Visibility filter
        if (isNonAdminUser(user)) {
            products = products.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getVisible()))
                    .toList();
        }

        // 2. Risk level filter
        if (isNonAdminUser(user)) {
            boolean shouldShowAll = Boolean.TRUE.equals(showAll);
            String riskProfile = user.getRiskProfile();
            boolean isRiskTaker = RiskConstants.RISK_TAKER.equalsIgnoreCase(riskProfile);

            if (!shouldShowAll && !isRiskTaker) {
                final int limitRisk = RiskConstants.MAX_RISK_LEVELS.getOrDefault(
                  riskProfile.toLowerCase(), RiskConstants.MAX_RISK_LEVELS.get(RiskConstants.RISK_TAKER));
                products = products.stream()
                        .filter(p -> p.getRiskLevel() <= limitRisk)
                        .toList();
            }
        }

        // 3. Type filter
        if (type != null && !type.isBlank()) {
            products = products.stream()
                    .filter(p -> type.equalsIgnoreCase(p.getType()))
                    .toList();
        }

        // 4. Search query filter
        if (searchQuery != null && !searchQuery.isBlank()) {
            String query = searchQuery.toLowerCase();
            products = products.stream()
                    .filter(p -> containsIgnoreCase(p.getName(), query) || containsIgnoreCase(p.getIssuer(), query))
                    .toList();
        }

        // 5. Dashboard Summary limit
        if (Boolean.TRUE.equals(dashboardSummary)) {
            products = products.stream()
                    .limit(ProductConstants.SUMMARY_COUNT)
                    .toList();
        }

        // 6. Pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), products.size());

        if (start > products.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, products.size());
        }

        List<Product> pageContent = products.subList(start, end);
        return new PageImpl<>(pageContent, pageable, products.size());
    }

    private static boolean containsIgnoreCase(String source, String query) {
        return source.toLowerCase().contains(query);
    }

    public Product updateProductVisibility(UUID id, Boolean visibility) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));
        product.setVisible(visibility);
        return productRepository.save(product);
    }

    public Product getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CoreThrowHandler(ApiError.ITEM_NOT_FOUND));

        if (!Boolean.TRUE.equals(product.getVisible())) {
            throw new CoreThrowHandler(ApiError.ITEM_NOT_FOUND);
        }

        return product;
    }
}


