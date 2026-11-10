package com.raviranjan.os.dto.integration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class GfgManualImportRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @Min(0)
    private int codingScore;

    @Min(0)
    private int totalProblemsSolved;

    private int instituteRank;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getCodingScore() { return codingScore; }
    public void setCodingScore(int codingScore) { this.codingScore = codingScore; }
    public int getTotalProblemsSolved() { return totalProblemsSolved; }
    public void setTotalProblemsSolved(int totalProblemsSolved) { this.totalProblemsSolved = totalProblemsSolved; }
    public int getInstituteRank() { return instituteRank; }
    public void setInstituteRank(int instituteRank) { this.instituteRank = instituteRank; }
}
