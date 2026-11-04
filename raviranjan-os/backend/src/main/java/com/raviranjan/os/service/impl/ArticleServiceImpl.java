package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.Article;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.domain.entity.ArticleTag;
import com.raviranjan.os.domain.enums.ArticleStatus;
import com.raviranjan.os.dto.writing.ArticleCreateRequest;
import com.raviranjan.os.dto.writing.ArticleResponse;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ArticleMapper;
import com.raviranjan.os.repository.ArticleRepository;
import com.raviranjan.os.repository.ArticleTagRepository;
import com.raviranjan.os.repository.ProjectRepository;
import com.raviranjan.os.service.ArticleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleTagRepository tagRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperActivityRepository activityRepository;
    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(
            ArticleRepository articleRepository,
            ArticleTagRepository tagRepository,
            ProjectRepository projectRepository,
            DeveloperActivityRepository activityRepository,
            ArticleMapper articleMapper
    ) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.projectRepository = projectRepository;
        this.activityRepository = activityRepository;
        this.articleMapper = articleMapper;
    }

    @Override
    @CacheEvict(value = {"articles", "unified_activity"}, allEntries = true)
    public ArticleResponse createArticle(ArticleCreateRequest request) {
        String cleanSlug = request.getSlug() != null && !request.getSlug().isBlank()
                ? request.getSlug().trim()
                : request.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");

        Article article = articleRepository.findBySlugAndDeletedAtIsNull(cleanSlug).orElseGet(Article::new);
        article.setDeletedAt(null);
        article.setTitle(request.getTitle().trim());
        article.setSlug(cleanSlug);
        article.setSummary(request.getSummary() != null ? request.getSummary() : request.getTitle());
        article.setContent(request.getContent());
        article.setCoverImageUrl(request.getCoverImageUrl());
        article.setStatus(request.getStatus() != null ? request.getStatus() : ArticleStatus.PUBLISHED);
        article.setPublishedAt(article.getStatus() == ArticleStatus.PUBLISHED ? (request.getPublishedAt() != null ? request.getPublishedAt() : Instant.now()) : null);
        article.setSeoTitle(request.getSeoTitle());
        article.setSeoDescription(request.getSeoDescription());
        article.setCanonicalUrl(request.getCanonicalUrl());
        article.setReadingTimeMinutes(request.getReadingTimeMinutes() > 0 ? request.getReadingTimeMinutes() : 5);

        if (request.getRelatedProjectId() != null) {
            article.setRelatedProject(projectRepository.findByIdAndDeletedAtIsNull(request.getRelatedProjectId()).orElse(null));
        }

        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            Set<ArticleTag> tags = new HashSet<>();
            for (String tagName : request.getTagNames()) {
                String slug = tagName.toLowerCase().replaceAll("[^a-z0-9]", "-");
                ArticleTag tag = tagRepository.findBySlug(slug).orElseGet(() -> {
                    ArticleTag newTag = new ArticleTag();
                    newTag.setName(tagName);
                    newTag.setSlug(slug);
                    return tagRepository.save(newTag);
                });
                tags.add(tag);
            }
            article.setTags(tags);
        }

        Article saved = articleRepository.save(article);
        try {
            String extKey = "article-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("WRITING", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.BLOG_PUBLISHED);
            act.setSourcePlatform("WRITING");
            act.setExternalId(extKey);
            act.setTitle("Published Article: " + saved.getTitle());
            act.setDescription(saved.getSummary());
            act.setExternalUrl("#writing");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return articleMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(value = {"articles", "unified_activity"}, allEntries = true)
    public ArticleResponse updateArticle(UUID id, ArticleCreateRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        article.setDeletedAt(null);
        if (request.getTitle() != null && !request.getTitle().isBlank()) article.setTitle(request.getTitle().trim());
        if (request.getSlug() != null && !request.getSlug().isBlank()) article.setSlug(request.getSlug().trim());
        if (request.getSummary() != null) article.setSummary(request.getSummary());
        if (request.getContent() != null && !request.getContent().isBlank()) article.setContent(request.getContent());
        if (request.getStatus() != null) {
            article.setStatus(request.getStatus());
            if (request.getStatus() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
                article.setPublishedAt(Instant.now());
            }
        }
        if (request.getReadingTimeMinutes() > 0) article.setReadingTimeMinutes(request.getReadingTimeMinutes());
        Article saved = articleRepository.save(article);
        try {
            String extKey = "article-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("WRITING", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.BLOG_PUBLISHED);
            act.setSourcePlatform("WRITING");
            act.setExternalId(extKey);
            act.setTitle("Published Article: " + saved.getTitle());
            act.setDescription(saved.getSummary());
            act.setExternalUrl("#writing");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return articleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "articles", key = "#slug")
    public ArticleResponse getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));
        return articleMapper.toResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getArticleById(UUID id) {
        Article article = articleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        return articleMapper.toResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getArticles(ArticleStatus status, String search, Pageable pageable) {
        Page<Article> page;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch) {
            String q = search.trim().toLowerCase();
            page = articleRepository.findAll((root, query, cb) -> {
                jakarta.persistence.criteria.Predicate notDeleted = cb.isNull(root.get("deletedAt"));
                jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + q + "%"),
                        cb.like(cb.lower(root.get("summary")), "%" + q + "%")
                );
                if (status != null) {
                    return cb.and(notDeleted, searchPredicate, cb.equal(root.get("status"), status));
                } else {
                    return cb.and(notDeleted, searchPredicate);
                }
            }, pageable);
        } else if (status != null) {
            page = articleRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        } else {
            page = articleRepository.findAllByDeletedAtIsNull(pageable);
        }

        return PageResponse.from(page.map(articleMapper::toResponse));
    }

    @Override
    @CacheEvict(value = {"articles", "unified_activity"}, allEntries = true)
    public void deleteArticle(UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        try {
            String extKey = "article-" + article.getId();
            String aTitle = article.getTitle();
            var acts = activityRepository.findAll().stream()
                    .filter(a -> extKey.equals(a.getExternalId()) || (aTitle != null && a.getTitle() != null && a.getTitle().contains(aTitle)))
                    .toList();
            if (!acts.isEmpty()) activityRepository.deleteAll(acts);
        } catch (Exception ignored) {}
        if (article.getTags() != null) {
            article.getTags().clear();
        }
        articleRepository.saveAndFlush(article);
        articleRepository.delete(article);
    }
}
