package com.techno.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for ProjectLaborRequestDetail entity.
 *
 * Combines request number and sequence number to uniquely identify
 * each detail line in a labor request.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectLaborRequestDetailId implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "request_no")
    private Long requestNo;

    @Column(name = "sequence_no")
    private Integer sequenceNo;
}
