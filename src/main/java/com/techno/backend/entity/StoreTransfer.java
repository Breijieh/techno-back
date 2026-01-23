package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "STORE_TRANSFERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreTransfer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 50)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_store_code", nullable = false)
    private ProjectStore fromStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_store_code", nullable = false)
    private ProjectStore toStore;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "transfer_status", nullable = false, length = 20)
    @Builder.Default
    private String transferStatus = "PENDING";

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "storeTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoreTransferLine> transferLines = new ArrayList<>();

    public void addTransferLine(StoreTransferLine line) {
        transferLines.add(line);
        line.setStoreTransfer(this);
    }
}
