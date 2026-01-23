package com.techno.backend.service;

import com.techno.backend.dto.report.ReportRequest;
import com.techno.backend.entity.Employee;
import com.techno.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for generating document-related reports.
 *
 * Implements 2 document reports:
 * 1. Expiring Documents - All documents (passports and residencies) expiring in date range
 * 2. Expiring Passports - Only passports expiring in date range
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 12 - Reports
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentReportService {

    private final EmployeeRepository employeeRepository;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    /**
     * Generate Expiring Documents Report.
     *
     * Shows all documents (passports and residencies) expiring within a date range.
     * Default range: next 14 days if not specified.
     *
     * @param request Report request with optional date range (defaults to next 14 days)
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateExpiringDocuments(ReportRequest request) {
        log.info("Generating Expiring Documents Report");

        // Default to next 14 days if date range not provided
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now().plusDays(14);
        }

        // Get all active employees
        List<Employee> employees = employeeRepository.findAllActiveEmployees();

        String title = "ØªÙ‚Ø±ÙŠØ± Ø§Ù„Ù…Ø³ØªÙ†Ø¯Ø§Øª Ø§Ù„Ù…Ù†ØªÙ‡ÙŠØ© Ø§Ù„ØµÙ„Ø§Ø­ÙŠØ©";
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø±Ù‚Ù… Ø§Ù„Ù‡ÙˆÙŠØ©",
                "Ù†ÙˆØ¹ Ø§Ù„Ù…Ø³ØªÙ†Ø¯",
                "Ø±Ù‚Ù… Ø§Ù„Ù…Ø³ØªÙ†Ø¯",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡",
                "Ø§Ù„Ø£ÙŠØ§Ù… Ø­ØªÙ‰ Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡",
                "Ø§Ù„Ø­Ø§Ù„Ø©"
        );

        List<List<Object>> data = new ArrayList<>();
        int expiredCount = 0;
        int expiringCount = 0;

        for (Employee employee : employees) {
            // Check passport
            if (employee.getPassportExpiryDate() != null) {
                LocalDate expiryDate = employee.getPassportExpiryDate();
                if (!expiryDate.isBefore(startDate) && !expiryDate.isAfter(endDate)) {
                    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
                    String status = daysUntil < 0 ? "EXPIRED" : 
                                  (daysUntil <= 7 ? "CRITICAL" : "WARNING");
                    
                    if (daysUntil < 0) expiredCount++;
                    else expiringCount++;

                    data.add(Arrays.asList(
                            employee.getEmployeeNo(),
                            employee.getEmployeeName(),
                            employee.getNationalId(),
                            "Passport",
                            employee.getPassportNo() != null ? employee.getPassportNo() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                            expiryDate,
                            daysUntil,
                            status
                    ));
                }
            }

            // Check residency
            if (employee.getResidencyExpiryDate() != null) {
                LocalDate expiryDate = employee.getResidencyExpiryDate();
                if (!expiryDate.isBefore(startDate) && !expiryDate.isAfter(endDate)) {
                    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
                    String status = daysUntil < 0 ? "EXPIRED" : 
                                  (daysUntil <= 7 ? "CRITICAL" : "WARNING");
                    
                    if (daysUntil < 0) expiredCount++;
                    else expiringCount++;

                    data.add(Arrays.asList(
                            employee.getEmployeeNo(),
                            employee.getEmployeeName(),
                            employee.getNationalId(),
                            "Residency/Iqama",
                            employee.getResidencyNo() != null ? employee.getResidencyNo() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                            expiryDate,
                            daysUntil,
                            status
                    ));
                }
            }
        }

        // Sort by expiry date (earliest first)
        data.sort((a, b) -> {
            LocalDate dateA = (LocalDate) a.get(5);
            LocalDate dateB = (LocalDate) b.get(5);
            return dateA.compareTo(dateB);
        });

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù…Ø³ØªÙ†Ø¯Ø§Øª", data.size());
        metadata.put("Ø§Ù„Ù…Ø³ØªÙ†Ø¯Ø§Øª Ø§Ù„Ù…Ù†ØªÙ‡ÙŠØ©", expiredCount);
        metadata.put("Ø§Ù„Ù…Ø³ØªÙ†Ø¯Ø§Øª Ø§Ù„Ù…Ù†ØªÙ‡ÙŠØ© Ù‚Ø±ÙŠØ¨Ø§Ù‹", expiringCount);
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", startDate + " Ø¥Ù„Ù‰ " + endDate);
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    /**
     * Generate Expiring Passports Report.
     *
     * Shows only passports expiring within a date range.
     * Default range: next 14 days if not specified.
     *
     * @param request Report request with optional date range (defaults to next 14 days)
     * @return Report file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateExpiringPassports(ReportRequest request) {
        log.info("Generating Expiring Passports Report");

        // Default to next 14 days if date range not provided
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now().plusDays(14);
        }

        // Get all active employees
        List<Employee> employees = employeeRepository.findAllActiveEmployees();

        String title = "ØªÙ‚Ø±ÙŠØ± Ø§Ù„Ø¬ÙˆØ§Ø²Ø§Øª Ø§Ù„Ù…Ù†ØªÙ‡ÙŠØ© Ø§Ù„ØµÙ„Ø§Ø­ÙŠØ©";
        List<String> headers = Arrays.asList(
                "Ø±Ù‚Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø§Ø³Ù… Ø§Ù„Ù…ÙˆØ¸Ù",
                "Ø±Ù‚Ù… Ø§Ù„Ù‡ÙˆÙŠØ©",
                "Ø§Ù„Ø¬Ù†Ø³ÙŠØ©",
                "Ø±Ù‚Ù… Ø¬ÙˆØ§Ø² Ø§Ù„Ø³ÙØ±",
                "ØªØ§Ø±ÙŠØ® Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡",
                "Ø§Ù„Ø£ÙŠØ§Ù… Ø­ØªÙ‰ Ø§Ù„Ø§Ù†ØªÙ‡Ø§Ø¡",
                "Ø§Ù„Ø­Ø§Ù„Ø©"
        );

        List<List<Object>> data = new ArrayList<>();
        int expiredCount = 0;
        int expiringCount = 0;

        for (Employee employee : employees) {
            if (employee.getPassportExpiryDate() != null) {
                LocalDate expiryDate = employee.getPassportExpiryDate();
                if (!expiryDate.isBefore(startDate) && !expiryDate.isAfter(endDate)) {
                    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
                    String status = daysUntil < 0 ? "EXPIRED" : 
                                  (daysUntil <= 7 ? "CRITICAL" : "WARNING");
                    
                    if (daysUntil < 0) expiredCount++;
                    else expiringCount++;

                    data.add(Arrays.asList(
                            employee.getEmployeeNo(),
                            employee.getEmployeeName(),
                            employee.getNationalId(),
                            employee.getNationality() != null ? employee.getNationality() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                            employee.getPassportNo() != null ? employee.getPassportNo() : "ØºÙŠØ± Ù…ØªØ§Ø­",
                            expiryDate,
                            daysUntil,
                            status
                    ));
                }
            }
        }

        // Sort by expiry date (earliest first)
        data.sort((a, b) -> {
            LocalDate dateA = (LocalDate) a.get(5);
            LocalDate dateB = (LocalDate) b.get(5);
            return dateA.compareTo(dateB);
        });

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ø¬ÙˆØ§Ø²Ø§Øª", data.size());
        metadata.put("Ø§Ù„Ø¬ÙˆØ§Ø²Ø§Øª Ø§Ù„Ù…Ù†ØªÙ‡ÙŠØ©", expiredCount);
        metadata.put("Ø§Ù„Ø¬ÙˆØ§Ø²Ø§Øª Ø§Ù„Ù…Ù†ØªÙ‡ÙŠØ© Ù‚Ø±ÙŠØ¨Ø§Ù‹", expiringCount);
        metadata.put("Ù†Ø·Ø§Ù‚ Ø§Ù„ØªØ§Ø±ÙŠØ®", startDate + " Ø¥Ù„Ù‰ " + endDate);
        metadata.put("ØªÙ… Ø§Ù„Ø¥Ù†Ø´Ø§Ø¡ ÙÙŠ", LocalDate.now());

        return generateReport(title, headers, data, metadata, request);
    }

    // Helper methods

    private byte[] generateReport(String title, List<String> headers, List<List<Object>> data,
                                  Map<String, Object> metadata, ReportRequest request) {
        if ("EXCEL".equalsIgnoreCase(request.getNormalizedFormat())) {
            return excelReportService.generateReport(title, headers, data, metadata);
        } else {
            return pdfReportService.generateReport(title, headers, data, metadata);
        }
    }
}


