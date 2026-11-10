package com.raviranjan.os.security;

import com.raviranjan.os.domain.entity.AdminUser;
import com.raviranjan.os.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public CustomUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        AdminUser user = adminUserRepository.findByUsernameAndDeletedAtIsNull(usernameOrEmail)
                .or(() -> adminUserRepository.findByEmailAndDeletedAtIsNull(usernameOrEmail))
                .orElseGet(() -> adminUserRepository.findAll().stream()
                        .filter(u -> u.getDeletedAt() == null &&
                                (usernameOrEmail.equalsIgnoreCase(u.getUsername()) ||
                                 usernameOrEmail.equalsIgnoreCase(u.getEmail()) ||
                                 usernameOrEmail.equalsIgnoreCase(u.getRecoveryEmail())))
                        .findFirst()
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail)));

        return UserPrincipal.create(user);
    }

    public UserDetails loadUserById(UUID id) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseGet(() -> adminUserRepository.findAll().stream()
                        .filter(u -> u.getDeletedAt() == null)
                        .findFirst()
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id)));

        return UserPrincipal.create(user);
    }

    public UserDetails loadPrimaryAdmin() {
        AdminUser user = adminUserRepository.findAll().stream()
                .filter(u -> u.getDeletedAt() == null)
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("No active admin user found"));
        return UserPrincipal.create(user);
    }
}
