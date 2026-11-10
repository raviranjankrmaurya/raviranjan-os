package com.raviranjan.os.dto.project;

import com.raviranjan.os.domain.enums.TaskPriority;
import com.raviranjan.os.domain.enums.TaskStatus;
import java.util.UUID;

public class TaskDto {
    private UUID id;
    private UUID milestoneId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private int displayOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMilestoneId() { return milestoneId; }
    public void setMilestoneId(UUID milestoneId) { this.milestoneId = milestoneId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
