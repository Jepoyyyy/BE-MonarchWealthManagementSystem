package com.indivaragroup.jdt17wms.repositories;

import com.indivaragroup.jdt17wms.models.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductPriceRepository extends JpaRepository<ProductPrice, UUID> {
  List<ProductPrice> findAllByProductIdInAndRecordedDateLessThanEqual(Collection<UUID> productIds, LocalDate recordedDate);

}
