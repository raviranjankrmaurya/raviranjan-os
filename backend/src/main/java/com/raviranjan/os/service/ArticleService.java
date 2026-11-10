package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ArticleStatus;
import com.raviranjan.os.dto.writing.ArticleCreateRequest;
import com.raviranjan.os.dto.writing.ArticleResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ArticleService {
    ArticleResponse createArticle(ArticleCreateRequest request);
    ArticleResponse updateArticle(UUID id, ArticleCreateRequest request);
    ArticleResponse getArticleBySlug(String slug);
    ArticleResponse getArticleById(UUID id);
    PageResponse<ArticleResponse> getArticles(ArticleStatus status, String search, Pageable pageable);
    void deleteArticle(UUID id);
}
