package com.techno.backend.service;

import com.techno.backend.dto.RegisterRequest;
import com.techno.backend.dto.ResetPasswordRequest;
import com.techno.backend.dto.UserInfoResponse;
import com.techno.backend.dto.UserListResponse;
import com.techno.backend.dto.UserUpdateRequest;
import com.techno.backend.entity.UserAccount;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.UserAccountRepository;
import com.techno.backend.repository.ProjectRepository;
import com.techno.backend.entity.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * User Service
 * Handles user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Find user by username
     * 
     * @param username the username
     * @return UserAccount
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserAccount findByUsername(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود باسم المستخدم: " + username));
    }

    /**
     * Find user by username or National ID
     * Tries username first, then National ID if username not found
     * 
     * @param identifier the username or National ID
     * @return UserAccount
     * @throws ResourceNotFoundException if user not found with either identifier
     */
    @Transactional(readOnly = true)
    public UserAccount findByUsernameOrNationalId(String identifier) {
        // First try to find by username
        Optional<UserAccount> userByUsername = userAccountRepository.findByUsername(identifier);
        if (userByUsername.isPresent()) {
            log.debug("User found by username: {}", identifier);
            return userByUsername.get();
        }

        // If not found by username, try National ID
        Optional<UserAccount> userByNationalId = userAccountRepository.findByNationalId(identifier);
        if (userByNationalId.isPresent()) {
            log.debug("User found by National ID: {}", identifier);
            return userByNationalId.get();
        }

        // Neither found
        log.error("User not found with username or National ID: {}", identifier);
        throw new ResourceNotFoundException("المستخدم غير موجود باسم المستخدم أو رقم الهوية الوطنية: " + identifier);
    }

    /**
     * Find user by ID
     * 
     * @param id the user ID
     * @return UserAccount
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserAccount findById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود برقم: " + id));
    }

    /**
     * Find user by employee number
     * 
     * @param employeeNo the employee number
     * @return UserAccount
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserAccount findByEmployeeNo(Long employeeNo) {
        return userAccountRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود برقم الموظف: " + employeeNo));
    }

    /**
     * Check if username exists
     * 
     * @param username the username
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userAccountRepository.existsByUsername(username);
    }

    /**
     * Check if national ID exists
     * 
     * @param nationalId the national ID
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean existsByNationalId(String nationalId) {
        return userAccountRepository.existsByNationalId(nationalId);
    }

    /**
     * Create new user
     * 
     * @param request the registration request
     * @return created UserAccount
     * @throws BadRequestException if username or national ID already exists
     */
    @Transactional
    public UserAccount createUser(RegisterRequest request) {
        // Validate unique username
        if (existsByUsername(request.getUsername())) {
            throw new BadRequestException("اسم المستخدم موجود بالفعل: " + request.getUsername());
        }

        // Validate unique national ID
        if (existsByNationalId(request.getNationalId())) {
            throw new BadRequestException("رقم الهوية الوطنية موجود بالفعل: " + request.getNationalId());
        }

        // Create user account
        UserAccount userAccount = UserAccount.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nationalId(request.getNationalId())
                .userType(request.getUserType())
                .employeeNo(request.getEmployeeNo())
                .isActive('Y')
                .build();

        UserAccount saved = userAccountRepository.save(userAccount);
        log.info("User created successfully: {}", saved.getUsername());

        // Handle Project Assignments
        if (request.getEmployeeNo() != null) {
            handleProjectAssignments(saved, request.getAssignedProjectId(), request.getAssignedProjectIds());
        }

        return saved;
    }

    /**
     * Update last login date and time
     * 
     * @param userId the user ID
     */
    @Transactional
    public void updateLastLogin(Long userId) {
        UserAccount user = findById(userId);
        user.setLastLoginDate(LocalDate.now());
        user.setLastLoginTime(LocalTime.now());
        userAccountRepository.save(user);
        log.debug("Updated last login for user: {}", user.getUsername());
    }

    /**
     * Activate user
     * 
     * @param userId the user ID
     * @return activated UserAccount
     */
    @Transactional
    public UserAccount activateUser(Long userId) {
        UserAccount user = findById(userId);
        user.setIsActive('Y');
        UserAccount saved = userAccountRepository.save(user);
        log.info("User activated: {}", saved.getUsername());
        return saved;
    }

    /**
     * Deactivate user
     * 
     * @param userId the user ID
     * @return deactivated UserAccount
     */
    @Transactional
    public UserAccount deactivateUser(Long userId) {
        // Prevent deactivating self
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BadRequestException("لا يمكنك إلغاء تفعيل حسابك الخاص");
        }

        UserAccount user = findById(userId);
        user.setIsActive('N');
        UserAccount saved = userAccountRepository.save(user);
        log.info("User deactivated: {}", saved.getUsername());
        return saved;
    }

    /**
     * Get all users with pagination
     * 
     * @param page          page number (0-based)
     * @param size          page size
     * @param sortBy        field to sort by
     * @param sortDirection sort direction (asc/desc)
     * @return UserListResponse with paginated users
     */
    @Transactional(readOnly = true)
    public UserListResponse getAllUsers(int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching all users - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, sortDirection);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserAccount> userPage = userAccountRepository.findAll(pageable);
        List<UserInfoResponse> userResponses = userPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return UserListResponse.builder()
                .content(userResponses)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .size(userPage.getSize())
                .number(userPage.getNumber())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .build();
    }

    /**
     * Update user
     * 
     * @param userId  the user ID
     * @param request the update request
     * @return updated UserAccount
     * @throws BadRequestException if username already exists (excluding current
     *                             user)
     */
    @Transactional
    public UserAccount updateUser(Long userId, UserUpdateRequest request) {
        UserAccount user = findById(userId);

        // Validate username uniqueness (excluding current user)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (existsByUsername(request.getUsername())) {
                throw new BadRequestException("اسم المستخدم موجود بالفعل: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        // Update other fields
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        if (request.getEmployeeNo() != null) {
            user.setEmployeeNo(request.getEmployeeNo());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive() ? 'Y' : 'N');
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        UserAccount saved = userAccountRepository.save(user);
        log.info("User updated successfully: {}", saved.getUsername());

        // Handle Project Assignments
        if (user.getEmployeeNo() != null &&
                (request.getAssignedProjectId() != null
                        || (request.getAssignedProjectIds() != null && !request.getAssignedProjectIds().isEmpty()))) {
            handleProjectAssignments(saved, request.getAssignedProjectId(), request.getAssignedProjectIds());
        }

        return saved;
    }

    /**
     * Delete user
     * 
     * @param userId the user ID
     * @throws BadRequestException if trying to delete self
     */
    @Transactional
    public void deleteUser(Long userId) {
        // Prevent deleting self
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BadRequestException("لا يمكنك حذف حسابك الخاص");
        }

        UserAccount user = findById(userId);
        userAccountRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }

    /**
     * Reset user password
     * 
     * @param userId  the user ID
     * @param request the reset password request
     * @return the new password (if generated) or null
     */
    @Transactional
    public String resetPassword(Long userId, ResetPasswordRequest request) {
        UserAccount user = findById(userId);
        String newPassword;

        if (Boolean.TRUE.equals(request.getGeneratePassword())) {
            // Generate random password
            newPassword = generateRandomPassword();
        } else {
            newPassword = request.getNewPassword();
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new BadRequestException("كلمة المرور مطلوبة إذا لم يتم توليدها");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);
        log.info("Password reset for user: {}", user.getUsername());

        // Return the password so it can be shown to admin (only if generated)
        return Boolean.TRUE.equals(request.getGeneratePassword()) ? newPassword : null;
    }

    /**
     * Map UserAccount entity to UserInfoResponse DTO
     */
    private UserInfoResponse mapToResponse(UserAccount user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nationalId(user.getNationalId())
                .userType(user.getUserType().name())
                .employeeNo(user.getEmployeeNo())
                .isActive(user.getIsActive())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }

    /**
     * Get current logged-in user ID from security context
     * 
     * @return current user ID or null if not authenticated
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication
                    .getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) authentication
                        .getPrincipal();
                String username = userDetails.getUsername();
                Optional<UserAccount> user = userAccountRepository.findByUsername(username);
                return user.map(UserAccount::getUserId).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Generate a secure random password
     * 
     * @return generated password
     */
    private String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*";
        String allChars = upperCase + lowerCase + numbers + specialChars;

        Random random = new SecureRandom();
        StringBuilder password = new StringBuilder(12);

        // Ensure at least one character from each category
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill the rest randomly
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    /**
     * Handle project assignments for Project Managers and Regional Managers
     */
    private void handleProjectAssignments(UserAccount user, Long assignedProjectId, List<Long> assignedProjectIds) {
        if (user.getUserType() == UserAccount.UserType.PROJECT_MANAGER && assignedProjectId != null) {
            projectRepository.findById(assignedProjectId).ifPresent(project -> {
                // Clear previous project manager if needed or just overwrite
                project.setProjectMgr(user.getEmployeeNo());
                projectRepository.save(project);
                log.info("Assigned Project Manager {} to Project {}", user.getUsername(), project.getProjectCode());
            });
        } else if (user.getUserType() == UserAccount.UserType.REGIONAL_PROJECT_MANAGER && assignedProjectIds != null) {
            List<Project> projects = projectRepository.findAllById(assignedProjectIds);
            for (Project project : projects) {
                project.setRegionalMgr(user.getEmployeeNo());
                projectRepository.save(project);
                log.info("Assigned Regional Manager {} to Project {}", user.getUsername(), project.getProjectCode());
            }
        }
    }
}
