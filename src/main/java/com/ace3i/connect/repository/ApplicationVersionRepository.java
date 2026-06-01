package com.ace3i.connect.repository;

import com.ace3i.connect.entity.ApplicationVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationVersionRepository extends JpaRepository<ApplicationVersion, Long> {
    Optional<ApplicationVersion> findByVersionNumber(String versionNumber);
    Optional<ApplicationVersion> findByIsLatestMasterTrue();
}
