package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "site_settings")
public class SiteSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_group", nullable = false, length = 50)
    private String settingGroup = "GENERAL";

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public String getSettingGroup() { return settingGroup; }
    public void setSettingGroup(String settingGroup) { this.settingGroup = settingGroup; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
