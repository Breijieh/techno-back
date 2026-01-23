package com.techno.backend.repository;

import com.techno.backend.entity.StoreTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoreTransactionRepository extends JpaRepository<StoreTransaction, Long> {

    @Query("SELECT t FROM StoreTransaction t WHERE t.store.storeCode = :storeCode AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<StoreTransaction> findByStoreCode(Long storeCode);

    @Query("SELECT t FROM StoreTransaction t WHERE t.item.itemCode = :itemCode AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<StoreTransaction> findByItemCode(Long itemCode);

    @Query("SELECT t FROM StoreTransaction t WHERE t.store.storeCode = :storeCode AND t.item.itemCode = :itemCode AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<StoreTransaction> findByStoreAndItem(Long storeCode, Long itemCode);

    @Query("SELECT t FROM StoreTransaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<StoreTransaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
