package com.ace3i.connect.repository;

import com.ace3i.connect.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    @Query("SELECT u FROM AppUser u WHERE " +
           "(:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:role IS NULL OR u.role = :role)")
    Page<AppUser> searchUsers(@Param("keyword") String keyword, @Param("role") String role, Pageable pageable);
}
