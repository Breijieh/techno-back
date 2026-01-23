package com.techno.backend.repository;

import com.techno.backend.entity.StoreTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreTransferRepository extends JpaRepository<StoreTransfer, Long> {

    @Query("SELECT t FROM StoreTransfer t WHERE t.transferId = :transferId AND t.isDeleted = false")
    Optional<StoreTransfer> findById(Long transferId);

    @Query("SELECT t FROM StoreTransfer t WHERE t.fromStore.storeCode = :storeCode AND t.isDeleted = false ORDER BY t.transferDate DESC")
    List<StoreTransfer> findByFromStoreCode(Long storeCode);

    @Query("SELECT t FROM StoreTransfer t WHERE t.toStore.storeCode = :storeCode AND t.isDeleted = false ORDER BY t.transferDate DESC")
    List<StoreTransfer> findByToStoreCode(Long storeCode);

    @Query("SELECT t FROM StoreTransfer t WHERE t.transferStatus = :status AND t.isDeleted = false ORDER BY t.transferDate DESC")
    List<StoreTransfer> findByStatus(String status);

    @Query("SELECT COUNT(t) > 0 FROM StoreTransfer t WHERE t.transferNumber = :transferNumber AND t.isDeleted = false")
    boolean existsByTransferNumber(String transferNumber);
}
