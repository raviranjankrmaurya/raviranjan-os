package com.raviranjan.os.dto.project;

import com.raviranjan.os.domain.enums.DecisionStatus;
import java.time.Instant;
import java.util.UUID;

public class DecisionDto {
    private UUID id;
    private String decisionTitle;
    private String problemContext;
    private String decisionTaken;
    private String tradeOffsConsidered;
    private String consequences;
    private DecisionStatus status;
    private Instant decidedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
