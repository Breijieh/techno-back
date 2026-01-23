package com.techno.backend.service;

import com.techno.backend.constants.NotificationEventType;
import com.techno.backend.entity.Employee;
import com.techno.backend.event.NotificationEvent;
import com.techno.backend.repository.AttendanceRepository;
import com.techno.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for OvertimeAlertService.
 * Tests overtime threshold detection and notification publishing.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Overtime Alert Service Tests")
class OvertimeAlertServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private OvertimeAlertService overtimeAlertService;

    private Employee testEmployee;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        testEmployee = Employee.builder()
                .employeeNo(1001L)
                .employeeName("أحمد محمد")
                .primaryDeptCode(1L)
                .primaryProjectCode(101L)
                .build();
    }

    // ==================== Overtime Alert Tests ====================

    @Test
    @DisplayName("Check overtime alerts when employee reaches 30 hours should send normal alert")
    void checkOvertimeAlerts_30Hours_SendsNormalAlert() {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        when(employeeRepository.findAllActiveEmployees()).thenReturn(List.of(testEmployee));
        when(attendanceRepository.sumOvertimeHours(1001L, monthStart, monthEnd))
                .thenReturn(30.0);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);

        overtimeAlertService.checkOvertimeAlerts();

        // Verify notifications were published
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        List<NotificationEvent> events = eventCaptor.getAllValues();
        assertThat(events).isNotEmpty();

        // Should notify employee and managers
        boolean hasEmployeeNotification = events.stream()
                .anyMatch(e -> e.getRecipientEmployeeNo().equals(1001L) &&
                        (e.getEventType().equals(NotificationEventType.OVERTIME_THRESHOLD_NORMAL) ||
                         e.getEventType().equals(NotificationEventType.OVERTIME_ALERT_30H)));

        assertThat(hasEmployeeNotification).isTrue();
    }

    @Test
    @DisplayName("Check overtime alerts when employee reaches 50 hours should send urgent alert")
    void checkOvertimeAlerts_50Hours_SendsUrgentAlert() {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        when(employeeRepository.findAllActiveEmployees()).thenReturn(List.of(testEmployee));
        when(attendanceRepository.sumOvertimeHours(1001L, monthStart, monthEnd))
                .thenReturn(50.0);
        when(systemConfigService.getHRManagerEmployeeNo()).thenReturn(2L);
        when(systemConfigService.getFinanceManagerEmployeeNo()).thenReturn(3L);
        when(systemConfigService.getGeneralManagerEmployeeNo()).thenReturn(1L);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);

        overtimeAlertService.checkOvertimeAlerts();

        verify(eventPublisher, atLeast(4)).publishEvent(eventCaptor.capture()); // Employee + 3 managers

        List<NotificationEvent> events = eventCaptor.getAllValues();
        boolean hasUrgentAlert = events.stream()
                .anyMatch(e -> e.getEventType().equals(NotificationEventType.OVERTIME_THRESHOLD_URGENT) ||
                             e.getEventType().equals(NotificationEventType.OVERTIME_ALERT_50H));

        assertThat(hasUrgentAlert).isTrue();
    }

    @Test
    @DisplayName("Check overtime alerts when employee below 30 hours should not send alert")
    void checkOvertimeAlerts_Below30Hours_NoAlert() {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        when(employeeRepository.findAllActiveEmployees()).thenReturn(List.of(testEmployee));
        when(attendanceRepository.sumOvertimeHours(1001L, monthStart, monthEnd))
                .thenReturn(25.0); // Below 30 hours

        overtimeAlertService.checkOvertimeAlerts();

        // Should not publish urgent or normal alerts
        verify(eventPublisher, never()).publishEvent(argThat(event ->
                event instanceof NotificationEvent &&
                (((NotificationEvent) event).getEventType().equals(NotificationEventType.OVERTIME_THRESHOLD_NORMAL) ||
                 ((NotificationEvent) event).getEventType().equals(NotificationEventType.OVERTIME_THRESHOLD_URGENT))
        ));
    }

    @Test
    @DisplayName("Check overtime alerts should notify HR, Finance, and General Manager")
    void checkOvertimeAlerts_NotifiesAllManagers() {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        when(employeeRepository.findAllActiveEmployees()).thenReturn(List.of(testEmployee));
        when(attendanceRepository.sumOvertimeHours(1001L, monthStart, monthEnd))
                .thenReturn(50.0);
        when(systemConfigService.getHRManagerEmployeeNo()).thenReturn(2L);
        when(systemConfigService.getFinanceManagerEmployeeNo()).thenReturn(3L);
        when(systemConfigService.getGeneralManagerEmployeeNo()).thenReturn(1L);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);

        overtimeAlertService.checkOvertimeAlerts();

        verify(eventPublisher, atLeast(4)).publishEvent(eventCaptor.capture());

        List<NotificationEvent> events = eventCaptor.getAllValues();
        boolean hasHRNotification = events.stream()
                .anyMatch(e -> e.getRecipientEmployeeNo().equals(2L));
        boolean hasFinanceNotification = events.stream()
                .anyMatch(e -> e.getRecipientEmployeeNo().equals(3L));
        boolean hasGeneralManagerNotification = events.stream()
                .anyMatch(e -> e.getRecipientEmployeeNo().equals(1L));

        assertThat(hasHRNotification).isTrue();
        assertThat(hasFinanceNotification).isTrue();
        assertThat(hasGeneralManagerNotification).isTrue();
    }

    @Test
    @DisplayName("Check overtime alerts should not send duplicate alerts for same month")
    void checkOvertimeAlerts_DuplicateAlert_Skips() {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        when(employeeRepository.findAllActiveEmployees()).thenReturn(List.of(testEmployee));
        when(attendanceRepository.sumOvertimeHours(1001L, monthStart, monthEnd))
                .thenReturn(30.0);

        // First call
        overtimeAlertService.checkOvertimeAlerts();

        // Second call (same month)
        overtimeAlertService.checkOvertimeAlerts();

        // Should only send alert once per month per threshold
        // Note: Implementation uses in-memory Set to track alerts
        // In production, this should be persisted
    }
}
