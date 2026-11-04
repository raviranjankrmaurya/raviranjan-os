package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.Article;
import com.raviranjan.os.domain.entity.ArticleTag;
import com.raviranjan.os.dto.writing.ArticleResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ArticleMapper {

    public ArticleResponse toResponse(Article entity) {
        if (entity == null) return null;
        ArticleResponse res = new ArticleResponse();
        res.setId(entity.getId());
        res.setSlug(entity.getSlug());
        res.setTitle(entity.getTitle());
        res.setSummary(entity.getSummary());
        res.setContent(entity.getContent());
        res.setCoverImageUrl(entity.getCoverImageUrl());
        res.setStatus(entity.getStatus());
        res.setPublishedAt(entity.getPublishedAt());
        res.setSeoTitle(entity.getSeoTitle());
        res.setSeoDescription(entity.getSeoDescription());
        res.setCanonicalUrl(entity.getCanonicalUrl());
        res.setReadingTimeMinutes(entity.getReadingTimeMinutes());
        if (entity.getRelatedProject() != null) {
            res.setRelatedProjectId(entity.getRelatedProject().getId());
            res.setRelatedProjectTitle(entity.getRelatedProject().getTitle());
        }
        if (entity.getTags() != null) {
            res.setTags(entity.getTags().stream().map(ArticleTag::getName).collect(Collectors.toSet()));
        }
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }
}
