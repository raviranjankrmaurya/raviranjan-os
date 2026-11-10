package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "article_tags")
public class ArticleTag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
