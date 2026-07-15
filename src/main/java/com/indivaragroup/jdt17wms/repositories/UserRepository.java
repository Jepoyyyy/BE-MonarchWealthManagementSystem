package com.indivaragroup.jdt17wms.repositories;

import com.indivaragroup.jdt17wms.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  List<User> findByRiskProfile(String riskProfile);
  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);

}
