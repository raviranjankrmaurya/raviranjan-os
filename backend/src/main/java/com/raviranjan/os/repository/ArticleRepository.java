package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.Article;
import com.raviranjan.os.domain.enums.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID>, JpaSpecificationExecutor<Article> {

    Optional<Article> findBySlugAndDeletedAtIsNull(String slug);

    Optional<Article> findByIdAndDeletedAtIsNull(UUID id);

    Page<Article> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Article> findByStatusAndDeletedAtIsNull(ArticleStatus status, Pageable pageable);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
