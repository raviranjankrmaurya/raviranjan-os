package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.GitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, UUID> {
    Optional<GitHubRepository> findByGithubId(Long githubId);
    Optional<GitHubRepository> findByFullName(String fullName);
}
