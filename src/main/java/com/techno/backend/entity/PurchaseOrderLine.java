package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "PURCHASE_ORDER_LINES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", nullable = false)
    private StoreItem item;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 12, scale = 4)
    private BigDecimal lineTotal;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    @PreUpdate
    private void calculateLineTotal() {
        if (quantity != null && unitPrice != null) {
            this.lineTotal = quantity.multiply(unitPrice);
        }
    }
}
