package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "gfg_profiles")
public class GfgProfile extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "institute_rank")
    private int instituteRank = 0;

    @Column(name = "current_coding_score")
    private int currentCodingScore = 0;

    @Column(name = "total_problems_solved")
    private int totalProblemsSolved = 0;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getInstituteRank() { return instituteRank; }
    public void setInstituteRank(int instituteRank) { this.instituteRank = instituteRank; }
    public int getCurrentCodingScore() { return currentCodingScore; }
    public void setCurrentCodingScore(int currentCodingScore) { this.currentCodingScore = currentCodingScore; }
    public int getTotalProblemsSolved() { return totalProblemsSolved; }
    public void setTotalProblemsSolved(int totalProblemsSolved) { this.totalProblemsSolved = totalProblemsSolved; }
}
