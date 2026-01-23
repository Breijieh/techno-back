package com.techno.backend.security;

import com.techno.backend.entity.UserAccount;
import com.techno.backend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * User Details Service Implementation
 * Loads user from database and converts to Spring Security UserDetails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by username first, then by National ID
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .or(() -> userAccountRepository.findByNationalId(username))
                .orElseThrow(() -> {
                    log.error("User not found with username or National ID: {}", username);
                    return new UsernameNotFoundException("المستخدم غير موجود: " + username);
                });

        if (userAccount.getIsActive() == null || userAccount.getIsActive() != 'Y') {
            log.error("User is inactive: {}", username);
            throw new UsernameNotFoundException("حساب المستخدم غير نشط: " + username);
        }

        List<GrantedAuthority> authorities = getAuthorities(userAccount);
        
        return User.builder()
                .username(userAccount.getUsername())
                .password(userAccount.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(userAccount.getIsActive() != 'Y')
                .build();
    }

    /**
     * Get authorities based on user type
     * 
     * @param userAccount the user account
     * @return list of granted authorities
     */
    private List<GrantedAuthority> getAuthorities(UserAccount userAccount) {
        String role = "ROLE_" + userAccount.getUserType().name();
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}

