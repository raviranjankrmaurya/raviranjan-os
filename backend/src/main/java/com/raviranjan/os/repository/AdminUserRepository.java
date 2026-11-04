package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByUsernameAndDeletedAtIsNull(String username);
    Optional<AdminUser> findByEmailAndDeletedAtIsNull(String email);

    default Optional<AdminUser> findByUsername(String username) {
        return findByUsernameAndDeletedAtIsNull(username);
    }

    default Optional<AdminUser> findByEmail(String email) {
        return findByEmailAndDeletedAtIsNull(email);
    }

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
