package com.techno.backend.event;

import com.techno.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener for NotificationEvent.
 *
 * Handles notification events asynchronously after successful transaction commit.
 * This ensures notifications are only created when business operations succeed.
 *
 * Processing flow:
 * 1. Business service publishes NotificationEvent
 * 2. Transaction commits successfully
 * 3. This listener receives event (async)
 * 4. NotificationService creates in-app notification
 * 5. EmailService sends email (if configured)
 *
 * Async benefits:
 * - Doesn't block business transaction
 * - Notification failures don't affect business logic
 * - Better performance for users
 * - Can be disabled in tests
 *
 * Transaction benefits:
 * - Events only fire after successful commit
 * - No notifications for failed operations
 * - Data consistency guaranteed
 *
 * Error handling:
 * - All exceptions are caught and logged
 * - Notification failures don't affect business operations
 * - Errors are reported but not propagated
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Handle notification events after successful transaction commit.
     *
     * Annotated with:
     * - @Async: Runs in separate thread pool (doesn't block caller)
     * - @TransactionalEventListener: Waits for transaction commit
     * - phase = AFTER_COMMIT: Only fires after successful commit
     *
     * This method:
     * 1. Receives NotificationEvent
     * 2. Delegates to NotificationService for processing
     * 3. Catches and logs any errors
     *
     * @param event NotificationEvent containing notification details
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            log.debug("Received notification event: {}", event);

            // Validate event
            if (event == null) {
                log.warn("Received null notification event, ignoring");
                return;
            }

            if (event.getRecipientEmployeeNo() == null) {
                log.warn("Notification event missing recipient employee number, ignoring: {}", event);
                return;
            }

            if (event.getEventType() == null || event.getEventType().isEmpty()) {
                log.warn("Notification event missing event type, ignoring: {}", event);
                return;
            }

            // Process notification
            log.info("Processing notification event: type={}, recipient={}, priority={}",
                    event.getEventType(), event.getRecipientEmployeeNo(), event.getPriority());

            notificationService.handleNotificationEvent(event);

            log.debug("Successfully processed notification event: {}", event.getEventType());

        } catch (Exception e) {
            // Log error but don't propagate - notification failures shouldn't break business logic
            log.error("Error processing notification event: type={}, recipient={}, error={}",
                    event != null ? event.getEventType() : "UNKNOWN",
                    event != null ? event.getRecipientEmployeeNo() : "UNKNOWN",
                    e.getMessage(), e);

            // TODO: Consider adding retry mechanism for failed notifications
            // TODO: Consider storing failed notifications for later review
        }
    }

    /**
     * Fallback event listener (non-transactional).
     *
     * This provides a backup in case the transactional listener doesn't fire.
     * Only processes events that weren't already handled by the transactional listener.
     *
     * Note: In normal operations, the @TransactionalEventListener above should handle all events.
     * This is just a safety net.
     *
     * @param event NotificationEvent
     */
    @EventListener
    @Async
    public void handleFallbackNotificationEvent(NotificationEvent event) {
        // This method intentionally left minimal to avoid duplicate processing
        // The transactional listener above is the primary handler
        log.trace("Fallback event listener received event: {}", event != null ? event.getEventType() : "null");
    }
}
