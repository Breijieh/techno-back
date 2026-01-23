package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PURCHASE_ORDERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id")
    private Long poId;

    @Column(name = "po_number", nullable = false, unique = true, length = 50)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_code", nullable = false)
    private ProjectStore store;

    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "supplier_name", nullable = false, length = 250)
    private String supplierName;

    @Column(name = "total_amount", precision = 12, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "po_status", nullable = false, length = 20)
    @Builder.Default
    private String poStatus = "DRAFT";

    @Column(name = "approval_notes", length = 1000)
    private String approvalNotes;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderLine> orderLines = new ArrayList<>();

    public void calculateTotalAmount() {
        this.totalAmount = orderLines.stream()
                .map(line -> line.getQuantity().multiply(line.getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addOrderLine(PurchaseOrderLine line) {
        orderLines.add(line);
        line.setPurchaseOrder(this);
    }
}
