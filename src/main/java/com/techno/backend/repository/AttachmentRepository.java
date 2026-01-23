package com.techno.backend.repository;

import com.techno.backend.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Attachment entity.
 * Provides database access methods for file attachment management.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * Find all attachments for a specific reference (e.g., all employee documents)
     */
    @Query("SELECT a FROM Attachment a WHERE a.referenceType = :referenceType AND a.referenceId = :referenceId ORDER BY a.uploadedDate DESC")
    List<Attachment> findByReferenceTypeAndReferenceId(
        @Param("referenceType") String referenceType,
        @Param("referenceId") Long referenceId
    );

    /**
     * Find all employee attachments
     */
    @Query("SELECT a FROM Attachment a WHERE a.referenceType = 'EMPLOYEE' AND a.referenceId = :employeeNo ORDER BY a.uploadedDate DESC")
    List<Attachment> findByEmployeeNo(@Param("employeeNo") Long employeeNo);

    /**
     * Find all project attachments
     */
    @Query("SELECT a FROM Attachment a WHERE a.referenceType = 'PROJECT' AND a.referenceId = :projectCode ORDER BY a.uploadedDate DESC")
    List<Attachment> findByProjectCode(@Param("projectCode") Long projectCode);

    /**
     * Find all attachments by type
     */
    @Query("SELECT a FROM Attachment a WHERE a.referenceType = :referenceType ORDER BY a.uploadedDate DESC")
    List<Attachment> findByReferenceType(@Param("referenceType") String referenceType);

    /**
     * Find attachments uploaded by a specific user
     */
    @Query("SELECT a FROM Attachment a WHERE a.uploadedBy = :userId ORDER BY a.uploadedDate DESC")
    List<Attachment> findByUploadedBy(@Param("userId") Long userId);

    /**
     * Count attachments for a specific reference
     */
    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.referenceType = :referenceType AND a.referenceId = :referenceId")
    Long countByReferenceTypeAndReferenceId(
        @Param("referenceType") String referenceType,
        @Param("referenceId") Long referenceId
    );

    /**
     * Find all attachments by file type (e.g., all PDFs)
     */
    @Query("SELECT a FROM Attachment a WHERE a.fileType = :fileType ORDER BY a.uploadedDate DESC")
    List<Attachment> findByFileType(@Param("fileType") String fileType);

    /**
     * Delete all attachments for a specific reference
     */
    void deleteByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
