package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleTagRepository extends JpaRepository<ArticleTag, UUID> {
    Optional<ArticleTag> findBySlug(String slug);
}
