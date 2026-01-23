package com.techno.backend.repository;

import com.techno.backend.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT p FROM PurchaseOrder p WHERE p.poId = :poId AND p.isDeleted = false")
    Optional<PurchaseOrder> findById(Long poId);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.store.storeCode = :storeCode AND p.isDeleted = false ORDER BY p.poDate DESC")
    List<PurchaseOrder> findByStoreCode(Long storeCode);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.poStatus = :status AND p.isDeleted = false ORDER BY p.poDate DESC")
    List<PurchaseOrder> findByStatus(String status);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.poNumber = :poNumber AND p.isDeleted = false")
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    @Query("SELECT COUNT(p) > 0 FROM PurchaseOrder p WHERE p.poNumber = :poNumber AND p.isDeleted = false")
    boolean existsByPoNumber(String poNumber);
}
