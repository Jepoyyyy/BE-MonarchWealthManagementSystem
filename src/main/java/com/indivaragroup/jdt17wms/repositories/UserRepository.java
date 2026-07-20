package com.indivaragroup.jdt17wms.repositories;

import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.indivaragroup.jdt17wms.dto.utils.UserSecurityProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  @Query("SELECT u FROM User u WHERE " +
         "(:status IS NULL OR u.status = :status) AND " +
         "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
  Page<User> findByStatusAndSearch(@Param("status") String status, @Param("search") String search, Pageable pageable);

  @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND CAST(u.role AS string) = :role")
  long countByStatusAndRole(@Param("status") String status, @Param("role") String role);
  List<User> findByRiskProfile(String riskProfile);
  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);

  @Query("SELECT u.riskProfile as riskProfile, COUNT(u) as count FROM User u GROUP BY u.riskProfile")
  List<RiskProfileCount> countByRiskProfile();

  interface RiskProfileCount {
    String getRiskProfile();
    Long getCount();
  }

  @Query("SELECT u.id as id, u.name as name, u.email as email, u.role as role, (SELECT COUNT(o) FROM User o WHERE o.createdAt < u.createdAt) as priorCount FROM User u WHERE u.email = :email")
  Optional<UserSecurityProjection> findUserSecurityProjectionByEmail(@Param("email") String email);
}
