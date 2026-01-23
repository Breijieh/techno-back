package com.techno.backend.repository;

import com.techno.backend.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    @Query("SELECT g FROM GoodsReceipt g WHERE g.receiptId = :receiptId AND g.isDeleted = false")
    Optional<GoodsReceipt> findById(Long receiptId);

    @Query("SELECT g FROM GoodsReceipt g WHERE g.store.storeCode = :storeCode AND g.isDeleted = false ORDER BY g.receiptDate DESC")
    List<GoodsReceipt> findByStoreCode(Long storeCode);

    @Query("SELECT g FROM GoodsReceipt g WHERE g.purchaseOrder.poId = :poId AND g.isDeleted = false ORDER BY g.receiptDate DESC")
    List<GoodsReceipt> findByPurchaseOrderId(Long poId);

    @Query("SELECT COUNT(g) > 0 FROM GoodsReceipt g WHERE g.receiptNumber = :receiptNumber AND g.isDeleted = false")
    boolean existsByReceiptNumber(String receiptNumber);
}
