package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "STORE_TRANSACTIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_code", nullable = false)
    private ProjectStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", nullable = false)
    private StoreItem item;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // RECEIPT, ISSUE, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity; // Positive for IN, Negative for OUT

    @Column(name = "reference_type", length = 50)
    private String referenceType; // GOODS_RECEIPT, GOODS_ISSUE, TRANSFER

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
