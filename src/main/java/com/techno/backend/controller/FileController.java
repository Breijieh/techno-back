package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.entity.Attachment;
import com.techno.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for File Management.
 * Provides endpoints for uploading, downloading, and deleting file attachments.
 *
 * Base URL: /api/files
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

        private final FileStorageService fileStorageService;

        /**
         * Upload a file and attach it to a reference (employee, project, etc.)
         *
         * @param file           File to upload
         * @param referenceType  Reference type (EMPLOYEE, PROJECT, etc.)
         * @param referenceId    Reference ID
         * @param description    Optional description
         * @param authentication Current authenticated user
         * @return Uploaded attachment details
         */
        @PostMapping("/upload")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Attachment>> uploadFile(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("referenceType") String referenceType,
                        @RequestParam("referenceId") Long referenceId,
                        @RequestParam(value = "description", required = false) String description,
                        Authentication authentication) {

                log.info("POST /api/files/upload - file: {}, ref: {}/{}, user: {}",
                                file.getOriginalFilename(), referenceType, referenceId, authentication.getName());

                // Get current user ID (simplified - in real app, get from UserService)
                Long uploadedBy = 1L; // TODO: Get from authentication context

                Attachment attachment = fileStorageService.uploadFile(file, referenceType, referenceId, uploadedBy,
                                description);

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                                "تم رفع الملف بنجاح",
                                attachment));
        }

        /**
         * Download file by attachment ID
         *
         * @param id Attachment ID
         * @return File resource
         */
        @GetMapping("/{id}/download")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
        public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
                log.info("GET /api/files/{}/download", id);

                Attachment attachment = fileStorageService.getAttachment(id);
                Resource resource = fileStorageService.downloadFile(id);

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(attachment.getFileType()))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + attachment.getFileName() + "\"")
                                .body(resource);
        }

        /**
         * Get attachment metadata by ID
         *
         * @param id Attachment ID
         * @return Attachment metadata
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<Attachment>> getAttachment(@PathVariable Long id) {
                log.info("GET /api/files/{}", id);

                Attachment attachment = fileStorageService.getAttachment(id);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم استرجاع المرفق بنجاح",
                                attachment));
        }

        /**
         * Get all attachments for a reference
         *
         * @param referenceType Reference type
         * @param referenceId   Reference ID
         * @return List of attachments
         */
        @GetMapping("/reference/{referenceType}/{referenceId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
        public ResponseEntity<ApiResponse<List<Attachment>>> getAttachmentsByReference(
                        @PathVariable String referenceType,
                        @PathVariable Long referenceId) {

                log.info("GET /api/files/reference/{}/{}", referenceType, referenceId);

                List<Attachment> attachments = fileStorageService.getAttachmentsByReference(referenceType, referenceId);

                return ResponseEntity.ok(ApiResponse.success(
                                String.format("تم العثور على %d مرفق", attachments.size()),
                                attachments));
        }

        /**
         * Delete attachment
         *
         * @param id Attachment ID
         * @return Success response
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
        public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable Long id) {
                log.info("DELETE /api/files/{}", id);

                fileStorageService.deleteAttachment(id);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم حذف المرفق بنجاح",
                                null));
        }

        /**
         * Delete all attachments for a reference
         *
         * @param referenceType Reference type
         * @param referenceId   Reference ID
         * @return Success response
         */
        @DeleteMapping("/reference/{referenceType}/{referenceId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<Void>> deleteAttachmentsByReference(
                        @PathVariable String referenceType,
                        @PathVariable Long referenceId) {

                log.info("DELETE /api/files/reference/{}/{}", referenceType, referenceId);

                fileStorageService.deleteAttachmentsByReference(referenceType, referenceId);

                return ResponseEntity.ok(ApiResponse.success(
                                "تم حذف جميع المرفقات بنجاح",
                                null));
        }
}
