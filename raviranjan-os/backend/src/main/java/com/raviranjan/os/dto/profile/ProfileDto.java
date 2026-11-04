package com.raviranjan.os.dto.profile;

import java.util.List;
import java.util.UUID;

public class ProfileDto {
    private UUID id;
    private String fullName;
    private String headline;
    private String bio;
    private String avatarUrl;
    private String location;
    private String email;
    private String phone;
    private boolean availableForHire;
    private String currentFocus;
    private String resumeUrl;
    private String systemStatus;
    private List<SocialLinkDto> socialLinks;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isAvailableForHire() { return availableForHire; }
    public void setAvailableForHire(boolean availableForHire) { this.availableForHire = availableForHire; }
    public String getCurrentFocus() { return currentFocus; }
    public void setCurrentFocus(String currentFocus) { this.currentFocus = currentFocus; }
    public String getResumeUrl() { return resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }
    public String getSystemStatus() { return systemStatus; }
    public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }
    public List<SocialLinkDto> getSocialLinks() { return socialLinks; }
    public void setSocialLinks(List<SocialLinkDto> socialLinks) { this.socialLinks = socialLinks; }
}
