package com.raviranjan.os.dto.career;

import java.time.LocalDate;
import java.util.UUID;

public class EducationDto {
    private UUID id;
    private String institution;
    private String degree;
    private String field = "Computer Science & Software Engineering";
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate;
    private String grade = "Enrolled";
    private String description;
    private int displayOrder = 1;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getField() { return (field != null && !field.isBlank()) ? field : degree; }
    public void setField(String field) { this.field = field; }
    public LocalDate getStartDate() { return startDate != null ? startDate : LocalDate.now(); }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
