package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.ResetPasswordRequest;
import com.techno.backend.dto.UserInfoResponse;
import com.techno.backend.dto.UserListResponse;
import com.techno.backend.dto.UserUpdateRequest;
import com.techno.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 * Handles user management endpoints
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users
     * Get all users with pagination
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @param sortBy sort field (default: userId)
     * @param sortDirection sort direction (default: asc)
     * @return paginated list of users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<UserListResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        log.info("GET /api/users - page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                page, size, sortBy, sortDirection);
        
        UserListResponse response = userService.getAllUsers(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع المستخدمين بنجاح", response));
    }

    /**
     * GET /api/users/{id}
     * Get user by ID
     * 
     * @param id the user ID
     * @return UserInfoResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserById(@PathVariable Long id) {
        log.info("GET /api/users/{}", id);
        
        var user = userService.findById(id);
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nationalId(user.getNationalId())
                .userType(user.getUserType().name())
                .employeeNo(user.getEmployeeNo())
                .isActive(user.getIsActive())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginTime(user.getLastLoginTime())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("تم استرجاع المستخدم بنجاح", response));
    }

    /**
     * PUT /api/users/{id}
     * Update user
     * 
     * @param id the user ID
     * @param request the update request
     * @return updated UserInfoResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /api/users/{} - request: {}", id, request);
        
        var user = userService.updateUser(id, request);
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nationalId(user.getNationalId())
                .userType(user.getUserType().name())
                .employeeNo(user.getEmployeeNo())
                .isActive(user.getIsActive())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginTime(user.getLastLoginTime())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("تم تحديث المستخدم بنجاح", response));
    }

    /**
     * DELETE /api/users/{id}
     * Delete user
     * 
     * @param id the user ID
     * @return success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/users/{}", id);
        
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("تم حذف المستخدم بنجاح", null));
    }

    /**
     * POST /api/users/{id}/reset-password
     * Reset user password
     * 
     * @param id the user ID
     * @param request the reset password request
     * @return success message with generated password (if generated)
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/users/{}/reset-password", id);
        
        String generatedPassword = userService.resetPassword(id, request);
        String message = generatedPassword != null 
                ? String.format("تم إعادة تعيين كلمة المرور بنجاح. كلمة المرور الجديدة: %s", generatedPassword)
                : "تم إعادة تعيين كلمة المرور بنجاح";
        
        return ResponseEntity.ok(ApiResponse.success(message, generatedPassword));
    }

    /**
     * PUT /api/users/{id}/activate
     * Activate user
     * 
     * @param id the user ID
     * @return updated UserInfoResponse
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> activateUser(@PathVariable Long id) {
        log.info("PUT /api/users/{}/activate", id);
        
        var user = userService.activateUser(id);
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nationalId(user.getNationalId())
                .userType(user.getUserType().name())
                .employeeNo(user.getEmployeeNo())
                .isActive(user.getIsActive())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginTime(user.getLastLoginTime())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("تم تفعيل المستخدم بنجاح", response));
    }

    /**
     * PUT /api/users/{id}/deactivate
     * Deactivate user (lock account)
     * 
     * @param id the user ID
     * @return updated UserInfoResponse
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> deactivateUser(@PathVariable Long id) {
        log.info("PUT /api/users/{}/deactivate", id);
        
        var user = userService.deactivateUser(id);
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nationalId(user.getNationalId())
                .userType(user.getUserType().name())
                .employeeNo(user.getEmployeeNo())
                .isActive(user.getIsActive())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginTime(user.getLastLoginTime())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("تم إلغاء تفعيل المستخدم بنجاح", response));
    }
}

