package com.indivaragroup.jdt17wms.repositories;

import com.indivaragroup.jdt17wms.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    long countByVisible(boolean visible);

    @Query("""
        SELECT p FROM Product p
        WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                               OR LOWER(p.issuer) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (:type IS NULL OR LOWER(p.type) = LOWER(CAST(:type AS string)))
        """)
    Page<Product> findAllAdmin(
        @Param("search") String search,
        @Param("type")   String type,
        Pageable pageable);
}
