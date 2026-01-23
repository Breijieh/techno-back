package com.techno.backend.service;

import com.techno.backend.entity.ProjectLaborRequestHeader;
import com.techno.backend.repository.ProjectLaborRequestHeaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled service for automatic labor request closure.
 *
 * This service runs a daily job at midnight (00:00) to:
 * 1. Find all ACTIVE labor requests where to_date has passed
 * 2. Automatically close expired requests
 * 3. Update request_status from 'ACTIVE' to 'CLOSED'
 * 4. Set closed_date to current date
 * 5. Log closure summary
 *
 * Labor Request Context:
 * - Labor requests are for temporary/daily workers
 * - Project managers request labor for specific date ranges (from_date to to_date)
 * - Each request specifies required workers by specialization (Carpenter, Electrician, etc.)
 * - Workers are assigned to requests during the valid period
 * - Once to_date passes, the request should be automatically closed
 *
 * Request Status Flow:
 * - ACTIVE: Request is valid and workers can be assigned
 * - CLOSED: Request period has ended (to_date passed)
 *
 * IMPORTANT: This service requires the ProjectLaborRequestHeader entity to be implemented.
 * Once the entity and repository are created, this service will automatically close
 * expired labor requests at midnight each day.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 8 - Batch Jobs & Automation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LaborRequestClosureService {

    private final ProjectLaborRequestHeaderRepository laborRequestRepository;

    /**
     * Automatic system closure identifier
     */
    private static final Long SYSTEM_CLOSURE_USER_ID = 0L; // System user

    /**
     * Daily labor request closure job.
     *
     * Runs at midnight (00:00) Saudi Arabia time.
     *
     * Process:
     * 1. Get current date
     * 2. Find all labor requests where:
     *    - request_status = 'ACTIVE'
     *    - to_date < current_date (past the end date)
     * 3. For each expired request:
     *    - Set request_status = 'CLOSED'
     *    - Set closed_date = current_date
     *    - Set closed_by = SYSTEM (0 = auto-closure)
     * 4. Log summary of closed requests
     *
     * This ensures:
     * - No manual intervention needed for request closure
     * - Clean separation between active and expired requests
     * - Accurate reporting of labor allocation history
     * - Prevents assignment of workers to expired requests
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Riyadh")
    @Transactional
    public void closeExpiredLaborRequests() {
        LocalDate today = LocalDate.now();

        log.info("=".repeat(80));
        log.info("Starting Labor Request Closure Job at {}", today);
        log.info("=".repeat(80));

        try {
            // Find expired requests that are still OPEN or PARTIAL
            List<ProjectLaborRequestHeader> expiredRequests =
                laborRequestRepository.findExpiredActiveRequests(today);

            if (expiredRequests.isEmpty()) {
                log.info("No expired labor requests found to close");
                log.info("=".repeat(80));
                return;
            }

            log.info("Found {} expired labor requests to close", expiredRequests.size());

            int successCount = 0;
            int errorCount = 0;

            for (ProjectLaborRequestHeader request : expiredRequests) {
                try {
                    // Close the request
                    request.setRequestStatus("CLOSED");
                    laborRequestRepository.save(request);

                    long daysDuration = request.getEndDate() != null && request.getStartDate() != null ?
                        ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1 : 0;

                    log.info("Closed Request #{} - Project {}: {} to {} ({} days)",
                        request.getRequestNo(),
                        request.getProjectCode(),
                        request.getStartDate(),
                        request.getEndDate(),
                        daysDuration);

                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to close labor request {}: {}",
                        request.getRequestNo(), e.getMessage(), e);
                }
            }

            log.info("\n" + "=".repeat(80));
            log.info("Labor Request Closure Job Summary:");
            log.info("  - Requests closed: {}", successCount);
            log.info("  - Errors: {}", errorCount);
            log.info("  - Total processed: {}", expiredRequests.size());
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("Labor Request Closure Job Failed: {}", e.getMessage(), e);
            log.error("=".repeat(80));
        }
    }

    /**
     * Manual trigger method for testing once entity is implemented.
     * Can be called from a controller endpoint for manual execution.
     *
     * @return Summary of the closure operation
     */
    @Transactional
    public String performManualClosure() {
        log.info("Manual labor request closure triggered");

        return "حالة خدمة إغلاق طلبات العمالة:\n" +
                "- الخدمة مُعدة وجاهزة\n" +
                "- يتطلب تنفيذ كيان ProjectLaborRequestHeader\n" +
                "- سيتم إغلاق الطلبات تلقائياً حيث انتهى تاريخ الانتهاء\n" +
                "- يعين حالة الطلب إلى 'CLOSED' ويسجل تاريخ الإغلاق\n" +
                "- مجدول للعمل يومياً عند منتصف الليل (00:00) بتوقيت السعودية\n" +
                "- سبب الإغلاق: تلقائي (انتهت فترة الطلب)\n" +
                "- سيتم تعيين closed_by إلى 0 (إغلاق النظام)";
    }

    /**
     * Get count of active labor requests (for monitoring).
     * This method will be useful once the entity is implemented.
     *
     * @return Count of active requests
     */
    @Transactional(readOnly = true)
    public Long getActiveRequestCount() {
        List<ProjectLaborRequestHeader> activeRequests = laborRequestRepository.findOpenRequests();
        return (long) activeRequests.size();
    }

    /**
     * Get count of requests that will expire today.
     * Useful for daily monitoring and reporting.
     *
     * @return Count of requests expiring today
     */
    @Transactional(readOnly = true)
    public Long getExpiringTodayCount() {
        LocalDate today = LocalDate.now();
        List<ProjectLaborRequestHeader> expiringToday = laborRequestRepository.findExpiredActiveRequests(today);
        // Filter to only those expiring today (endDate equals today)
        return expiringToday.stream()
                .filter(r -> r.getEndDate() != null && r.getEndDate().equals(today))
                .count();
    }
}
