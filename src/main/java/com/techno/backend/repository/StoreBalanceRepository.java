package com.techno.backend.repository;

import com.techno.backend.entity.StoreBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreBalanceRepository extends JpaRepository<StoreBalance, Long> {

    @Query("SELECT b FROM StoreBalance b WHERE b.balanceId = :balanceId AND b.isDeleted = false")
    Optional<StoreBalance> findById(Long balanceId);

    @Query("SELECT b FROM StoreBalance b WHERE b.store.storeCode = :storeCode AND b.item.itemCode = :itemCode AND b.isDeleted = false")
    Optional<StoreBalance> findByStoreAndItem(Long storeCode, Long itemCode);

    @Query("SELECT b FROM StoreBalance b WHERE b.store.storeCode = :storeCode AND b.isDeleted = false ORDER BY b.item.itemName")
    List<StoreBalance> findByStoreCode(Long storeCode);

    @Query("SELECT b FROM StoreBalance b WHERE b.item.itemCode = :itemCode AND b.isDeleted = false ORDER BY b.store.storeName")
    List<StoreBalance> findByItemCode(Long itemCode);
    
    @Query("SELECT b FROM StoreBalance b WHERE b.item.itemCode IN :itemCodes AND b.isDeleted = false")
    List<StoreBalance> findByItemCodeIn(List<Long> itemCodes);

    @Query("SELECT b FROM StoreBalance b WHERE b.quantityOnHand > 0 AND b.isDeleted = false")
    List<StoreBalance> findAllWithStock();

    @Query("SELECT b FROM StoreBalance b WHERE b.quantityOnHand < :threshold AND b.isDeleted = false")
    List<StoreBalance> findLowStockItems(BigDecimal threshold);

    @Query("SELECT b FROM StoreBalance b WHERE b.item.reorderLevel IS NOT NULL AND b.quantityOnHand <= b.item.reorderLevel AND b.isDeleted = false")
    List<StoreBalance> findItemsBelowReorderLevel();

    /**
     * Count balances by store code excluding deleted balances
     */
    @Query("SELECT COUNT(b) FROM StoreBalance b WHERE b.store.storeCode = :storeCode AND b.isDeleted = false")
    long countByStoreCodeAndIsDeletedFalse(Long storeCode);

    /**
     * Get available quantity directly from database using native query (bypasses JPA cache)
     * Returns: quantity_on_hand - COALESCE(quantity_reserved, 0)
     */
    @Query(value = "SELECT (b.quantity_on_hand - COALESCE(b.quantity_reserved, 0)) " +
           "FROM STORE_BALANCES b " +
           "WHERE b.store_code = :storeCode AND b.item_code = :itemCode AND b.is_deleted = false",
           nativeQuery = true)
    BigDecimal getAvailableQuantityNative(@Param("storeCode") Long storeCode,
                                         @Param("itemCode") Long itemCode);
}
