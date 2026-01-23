package com.techno.backend.exception;

import com.techno.backend.dto.labor.AssignmentOverlapInfo;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when an employee has overlapping labor assignments.
 * Carries structured information about the overlapping assignments.
 * 
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Getter
public class AssignmentOverlapException extends BadRequestException {
    
    private final List<AssignmentOverlapInfo> overlappingAssignments;
    
    public AssignmentOverlapException(String message, List<AssignmentOverlapInfo> overlappingAssignments) {
        super(message);
        this.overlappingAssignments = overlappingAssignments;
    }
}
