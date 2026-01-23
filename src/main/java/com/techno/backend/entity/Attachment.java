package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing file attachments in the system.
 * Maps to ATTACHMENTS table in database.
 *
 * Supports various reference types:
 * - EMPLOYEE: Employee documents (passport, residency, contracts)
 * - PO: Purchase order documents
 * - INVOICE: Invoice documents
 * - CONTRACT: Contract documents
 * - PROJECT: Project-related documents
 * - LEAVE: Leave request attachments (medical certificates)
 * - LOAN: Loan request documents
 *
 * File storage structure: /uploads/{year}/{month}/{filename}
 * Example: /uploads/2025/11/passport_12345.pdf
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Entity
@Table(name = "attachments", indexes = {
    @Index(name = "idx_attachment_reference", columnList = "reference_type, reference_id"),
    @Index(name = "idx_attachment_uploaded_by", columnList = "uploaded_by"),
    @Index(name = "idx_attachment_uploaded_date", columnList = "uploaded_date")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment extends BaseEntity {

    /**
     * Attachment ID - Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    /**
     * Reference type - what this attachment belongs to
     * Values: EMPLOYEE, PO, INVOICE, CONTRACT, PROJECT, LEAVE, LOAN, etc.
     */
    @NotBlank(message = "نوع المرجع مطلوب")
    @Size(max = 50, message = "نوع المرجع لا يجب أن يتجاوز 50 حرف")
    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    /**
     * Reference ID - the ID of the related entity
     * Example: employee_no, project_code, leave_id, etc.
     */
    @NotNull(message = "معرف المرجع مطلوب")
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    /**
     * Original file name
     * Example: "employee_passport.pdf", "medical_certificate.jpg"
     */
    @NotBlank(message = "اسم الملف مطلوب")
    @Size(max = 500, message = "اسم الملف لا يجب أن يتجاوز 500 حرف")
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /**
     * File storage path on server
     * Example: "/uploads/2025/11/abc123_employee_passport.pdf"
     */
    @NotBlank(message = "مسار الملف مطلوب")
    @Size(max = 1000, message = "مسار الملف لا يجب أن يتجاوز 1000 حرف")
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    /**
     * File size in bytes
     */
    @Min(value = 0, message = "حجم الملف لا يمكن أن يكون سالباً")
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * File MIME type
     * Examples: "application/pdf", "image/jpeg", "image/png", "application/vnd.ms-excel"
     */
    @Size(max = 50, message = "نوع الملف لا يجب أن يتجاوز 50 حرف")
    @Column(name = "file_type", length = 50)
    private String fileType;

    /**
     * User who uploaded this file
     * References USER_ACCOUNTS.user_id
     */
    @Column(name = "uploaded_by")
    private Long uploadedBy;

    /**
     * Timestamp when file was uploaded
     */
    @NotNull(message = "تاريخ الرفع مطلوب")
    @Column(name = "uploaded_date", nullable = false)
    @Builder.Default
    private LocalDateTime uploadedDate = LocalDateTime.now();

    /**
     * Optional description or notes about the attachment
     */
    @Size(max = 500, message = "الوصف لا يجب أن يتجاوز 500 حرف")
    @Column(name = "description", length = 500)
    private String description;

    // Relationships

    /**
     * Reference to uploader user account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "user_id",
                insertable = false, updatable = false)
    private UserAccount uploader;

    // Helper methods

    /**
     * Get file extension from filename
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Check if file is an image
     */
    public boolean isImage() {
        if (fileType == null) {
            return false;
        }
        return fileType.startsWith("image/");
    }

    /**
     * Check if file is a PDF
     */
    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(fileType);
    }

    /**
     * Check if file is an Excel file
     */
    public boolean isExcel() {
        if (fileType == null) {
            return false;
        }
        return fileType.contains("excel") || fileType.contains("spreadsheet");
    }

    /**
     * Check if file is a Word document
     */
    public boolean isWord() {
        if (fileType == null) {
            return false;
        }
        return fileType.contains("word") || fileType.contains("document");
    }

    /**
     * Get human-readable file size
     */
    public String getFileSizeFormatted() {
        if (fileSize == null) {
            return "0 بايت";
        }
        long size = fileSize;
        if (size < 1024) return size + " بايت";
        if (size < 1024 * 1024) return String.format("%.2f كيلوبايت", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f ميجابايت", size / (1024.0 * 1024));
        return String.format("%.2f جيجابايت", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Check if attachment is for an employee
     */
    public boolean isEmployeeAttachment() {
        return "EMPLOYEE".equalsIgnoreCase(referenceType);
    }

    /**
     * Check if attachment is for a project
     */
    public boolean isProjectAttachment() {
        return "PROJECT".equalsIgnoreCase(referenceType);
    }

    /**
     * Check if attachment is for a leave request
     */
    public boolean isLeaveAttachment() {
        return "LEAVE".equalsIgnoreCase(referenceType);
    }

    /**
     * Check if attachment is for a loan request
     */
    public boolean isLoanAttachment() {
        return "LOAN".equalsIgnoreCase(referenceType);
    }
}
