package com.raviranjan.os.dto.career;

import java.time.LocalDate;
import java.util.UUID;

public class AchievementDto {
    private UUID id;
    private String title;
    private String category = "COMPETITIVE_PROGRAMMING";
    private String organization = "Verified Platform";
    private LocalDate date = LocalDate.now();
    private String description;
    private String proofUrl;
    private String certificate;
    private boolean featured = true;
    private int displayOrder = 1;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public LocalDate getDate() { return date != null ? date : LocalDate.now(); }
    public void setDate(LocalDate date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
    public String getCertificate() { return certificate; }
    public void setCertificate(String certificate) { this.certificate = certificate; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
