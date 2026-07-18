package com.indivaragroup.jdt17wms.repositories;

import com.indivaragroup.jdt17wms.models.TransactionHistory;
import com.indivaragroup.jdt17wms.models.enums.TransactionAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, UUID> {
    List<TransactionHistory> findAllByUserId(UUID userId);
    List<TransactionHistory> findAllByAssetIdAndActionOrderByTransactionDateAsc(UUID assetId, TransactionAction action);
    List<TransactionHistory> findAllByAssetIdOrderByTransactionDateDesc(UUID assetId);
}
