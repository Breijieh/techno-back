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
@Table(name = "GOODS_ISSUES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "issue_number", nullable = false, unique = true, length = 50)
    private String issueNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_code", nullable = false)
    private ProjectStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_code", nullable = false)
    private Project project;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "issued_to", length = 250)
    private String issuedTo;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "goodsIssue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsIssueLine> issueLines = new ArrayList<>();

    public void addIssueLine(GoodsIssueLine line) {
        issueLines.add(line);
        line.setGoodsIssue(this);
    }
}
