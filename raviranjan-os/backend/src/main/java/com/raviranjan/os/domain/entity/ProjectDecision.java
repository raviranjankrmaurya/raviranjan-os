package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import com.raviranjan.os.domain.enums.DecisionStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_decisions")
public class ProjectDecision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "decision_title", nullable = false)
    private String decisionTitle;

    @Column(name = "problem_context", nullable = false, columnDefinition = "TEXT")
    private String problemContext;

    @Column(name = "decision_taken", nullable = false, columnDefinition = "TEXT")
    private String decisionTaken;

    @Column(name = "trade_offs_considered", nullable = false, columnDefinition = "TEXT")
    private String tradeOffsConsidered;

    @Column(columnDefinition = "TEXT")
    private String consequences;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DecisionStatus status = DecisionStatus.ACCEPTED;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt = Instant.now();

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getDecisionTitle() { return decisionTitle; }
    public void setDecisionTitle(String decisionTitle) { this.decisionTitle = decisionTitle; }
    public String getProblemContext() { return problemContext; }
    public void setProblemContext(String problemContext) { this.problemContext = problemContext; }
    public String getDecisionTaken() { return decisionTaken; }
    public void setDecisionTaken(String decisionTaken) { this.decisionTaken = decisionTaken; }
    public String getTradeOffsConsidered() { return tradeOffsConsidered; }
    public void setTradeOffsConsidered(String tradeOffsConsidered) { this.tradeOffsConsidered = tradeOffsConsidered; }
    public String getConsequences() { return consequences; }
    public void setConsequences(String consequences) { this.consequences = consequences; }
    public DecisionStatus getStatus() { return status; }
    public void setStatus(DecisionStatus status) { this.status = status; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
