package com.raviranjan.os.dto.profile;

import java.util.UUID;

public class SocialLinkDto {
    private UUID id;
    private String platform;
    private String url;
    private String handle;
    private String iconKey;
    private int displayOrder;
    private boolean visible;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
