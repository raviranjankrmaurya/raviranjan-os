package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.writing.ArticleCreateRequest;
import com.raviranjan.os.dto.writing.ArticleResponse;
import com.raviranjan.os.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Writing & Articles", description = "Technical essays and publication engine")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/api/articles")
    @Operation(summary = "List published articles (Public)")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> getPublishedArticles(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(articleService.getArticles(null, search, pageable)));
    }

    @GetMapping("/api/articles/{slug}")
    @Operation(summary = "Read article by slug (Public)")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getArticleBySlug(slug)));
    }

    @PostMapping("/api/admin/articles")
    @Operation(summary = "Create article (Admin)")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(@Valid @RequestBody ArticleCreateRequest request) {
        return new ResponseEntity<>(ApiResponse.success("Article saved to Neon DB", articleService.createArticle(request)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/articles/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update article (Admin)")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(@PathVariable UUID id, @RequestBody ArticleCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Article updated in Neon DB", articleService.updateArticle(id, request)));
    }

    @DeleteMapping("/api/admin/articles/{id}")
    @Operation(summary = "Delete article (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable UUID id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(ApiResponse.success("Article deleted from Neon DB", null));
    }
}
