package com.techno.backend.service;

import com.techno.backend.entity.Attachment;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service class for File Storage management.
 * Handles file upload, download, and deletion operations.
 *
 * Files are stored in the file system with the following structure:
 * /uploads/{year}/{month}/{filename}
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private final AttachmentRepository attachmentRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size-mb:10}")
    private long maxFileSizeMB;

    // Allowed file types
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "jpg", "jpeg", "png", "xls", "xlsx", "doc", "docx"
    );

    /**
     * Upload file and create attachment record
     */
    @Transactional
    public Attachment uploadFile(MultipartFile file, String referenceType, Long referenceId,
                                  Long uploadedBy, String description) {
        log.info("Uploading file: {} for {}/{}", file.getOriginalFilename(), referenceType, referenceId);

        // Validate file
        validateFile(file);

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            String uniqueFilename = generateUniqueFilename(originalFilename);

            // Create directory structure: uploads/YYYY/MM/
            LocalDateTime now = LocalDateTime.now();
            String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String month = now.format(DateTimeFormatter.ofPattern("MM"));

            Path uploadPath = Paths.get(uploadDir, year, month);
            Files.createDirectories(uploadPath);

            // Copy file to destination
            Path destinationPath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // Create relative path for database storage
            String relativePath = String.format("/%s/%s/%s/%s", uploadDir, year, month, uniqueFilename);

            // Create attachment record
            Attachment attachment = Attachment.builder()
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .fileName(originalFilename)
                    .filePath(relativePath)
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .uploadedBy(uploadedBy)
                    .uploadedDate(now)
                    .description(description)
                    .build();

            attachment = attachmentRepository.save(attachment);
            log.info("File uploaded successfully: {}", attachment.getAttachmentId());

            return attachment;

        } catch (IOException e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            throw new BadRequestException("فشل تحميل الملف: " + e.getMessage());
        }
    }

    /**
     * Download file by attachment ID
     */
    @Transactional(readOnly = true)
    public Resource downloadFile(Long attachmentId) {
        log.info("Downloading file with attachment ID: {}", attachmentId);

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("المرفق غير موجود برقم: " + attachmentId));

        try {
            Path filePath = Paths.get(attachment.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("File downloaded successfully: {}", attachment.getFileName());
                return resource;
            } else {
                log.error("File not found or not readable: {}", filePath);
                throw new ResourceNotFoundException("الملف غير موجود: " + attachment.getFileName());
            }
        } catch (MalformedURLException e) {
            log.error("Invalid file path: {}", attachment.getFilePath(), e);
            throw new BadRequestException("مسار الملف غير صالح: " + e.getMessage());
        }
    }

    /**
     * Get attachment metadata by ID
     */
    @Transactional(readOnly = true)
    public Attachment getAttachment(Long attachmentId) {
        log.info("Fetching attachment metadata: {}", attachmentId);
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("المرفق غير موجود برقم: " + attachmentId));
    }

    /**
     * Get all attachments for a reference
     */
    @Transactional(readOnly = true)
    public List<Attachment> getAttachmentsByReference(String referenceType, Long referenceId) {
        log.info("Fetching attachments for {}/{}", referenceType, referenceId);
        return attachmentRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    /**
     * Get all employee attachments
     */
    @Transactional(readOnly = true)
    public List<Attachment> getEmployeeAttachments(Long employeeNo) {
        log.info("Fetching attachments for employee: {}", employeeNo);
        return attachmentRepository.findByEmployeeNo(employeeNo);
    }

    /**
     * Delete attachment (deletes file and database record)
     */
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        log.info("Deleting attachment: {}", attachmentId);

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("المرفق غير موجود برقم: " + attachmentId));

        // Delete physical file
        try {
            Path filePath = Paths.get(attachment.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
            log.info("Physical file deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", attachment.getFilePath(), e);
            // Continue with database deletion even if file deletion fails
        }

        // Delete database record
        attachmentRepository.delete(attachment);
        log.info("Attachment deleted successfully: {}", attachmentId);
    }

    /**
     * Delete all attachments for a reference
     */
    @Transactional
    public void deleteAttachmentsByReference(String referenceType, Long referenceId) {
        log.info("Deleting all attachments for {}/{}", referenceType, referenceId);

        List<Attachment> attachments = attachmentRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);

        for (Attachment attachment : attachments) {
            // Delete physical file
            try {
                Path filePath = Paths.get(attachment.getFilePath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete physical file: {}", attachment.getFilePath(), e);
            }
        }

        // Delete database records
        attachmentRepository.deleteByReferenceTypeAndReferenceId(referenceType, referenceId);
        log.info("All attachments deleted for {}/{}", referenceType, referenceId);
    }

    // ===== Private Helper Methods =====

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("لا يمكن تحميل ملف فارغ");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("اسم الملف غير صالح");
        }

        // Validate file size
        long fileSizeBytes = file.getSize();
        long maxSizeBytes = maxFileSizeMB * 1024 * 1024;
        if (fileSizeBytes > maxSizeBytes) {
            throw new BadRequestException(
                    String.format("حجم الملف يتجاوز الحد الأقصى المسموح به وهو %d ميجابايت", maxFileSizeMB));
        }

        // Validate file extension
        String fileExtension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
            throw new BadRequestException(
                    "نوع الملف غير مسموح. الأنواع المسموحة: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "نوع المحتوى غير مسموح: " + contentType);
        }

        // Security: Check for path traversal attempts
        if (originalFilename.contains("..")) {
            throw new BadRequestException("اسم الملف يحتوي على تسلسل مسار غير صالح");
        }

        log.info("File validation passed: {} ({} bytes, {})", originalFilename, fileSizeBytes, contentType);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Generate unique filename to prevent collisions
     */
    private String generateUniqueFilename(String originalFilename) {
        String fileExtension = getFileExtension(originalFilename);
        String filenameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf("."));

        // Clean filename (remove special characters)
        filenameWithoutExtension = filenameWithoutExtension.replaceAll("[^a-zA-Z0-9_-]", "_");

        // Generate UUID
        String uuid = UUID.randomUUID().toString();

        // Combine: cleanedName_UUID.extension
        return String.format("%s_%s.%s", filenameWithoutExtension, uuid, fileExtension);
    }
}
