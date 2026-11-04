package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "leetcode_profiles")
public class LeetCodeProfile extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    private int ranking;
    private int reputation;

    @Column(name = "total_solved")
    private int totalSolved = 0;

    @Column(name = "easy_solved")
    private int easySolved = 0;

    @Column(name = "medium_solved")
    private int mediumSolved = 0;

    @Column(name = "hard_solved")
    private int hardSolved = 0;

    @Column(name = "acceptance_rate")
    private double acceptanceRate = 0.0;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getRanking() { return ranking; }
    public void setRanking(int ranking) { this.ranking = ranking; }
    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }
    public int getTotalSolved() { return totalSolved; }
    public void setTotalSolved(int totalSolved) { this.totalSolved = totalSolved; }
    public int getEasySolved() { return easySolved; }
    public void setEasySolved(int easySolved) { this.easySolved = easySolved; }
    public int getMediumSolved() { return mediumSolved; }
    public void setMediumSolved(int mediumSolved) { this.mediumSolved = mediumSolved; }
    public int getHardSolved() { return hardSolved; }
    public void setHardSolved(int hardSolved) { this.hardSolved = hardSolved; }
    public double getAcceptanceRate() { return acceptanceRate; }
    public void setAcceptanceRate(double acceptanceRate) { this.acceptanceRate = acceptanceRate; }
}
