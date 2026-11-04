package com.raviranjan.os.dto.ai;

public class KnowledgeSourceDto {
    private String title;
    private String url;
    private String sourceType;

    public KnowledgeSourceDto(String title, String url, String sourceType) {
        this.title = title;
        this.url = url;
        this.sourceType = sourceType;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSourceType() { return sourceType; }
}
