package com.raviranjan.os.dto.dsa;

import java.time.LocalDate;

public class ManualProblemCreateRequest {
    private String platform = "LEETCODE";
    private String title;
    private String problemName;
    private String slug;
    private String difficulty = "MEDIUM";
    private String problemUrl;
    private String topic;
    private String topicTags;
    private String approach;
    private String approachNotes;
    private String timeComplexity = "O(n)";
    private String spaceComplexity = "O(1)";
    private String solutionCode;
    private String codeSnippet;
    private String whatILearned;
    private LocalDate solvedDate = LocalDate.now();

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTitle() { return (title != null && !title.isBlank()) ? title : problemName; }
    public void setTitle(String title) { this.title = title; }
    public String getProblemName() { return (problemName != null && !problemName.isBlank()) ? problemName : title; }
    public void setProblemName(String problemName) { this.problemName = problemName; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getProblemUrl() { return problemUrl; }
    public void setProblemUrl(String problemUrl) { this.problemUrl = problemUrl; }
    public String getTopic() { return (topic != null && !topic.isBlank()) ? topic : topicTags; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getTopicTags() { return (topicTags != null && !topicTags.isBlank()) ? topicTags : topic; }
    public void setTopicTags(String topicTags) { this.topicTags = topicTags; }
    public String getApproach() { return (approach != null && !approach.isBlank()) ? approach : approachNotes; }
    public void setApproach(String approach) { this.approach = approach; }
    public String getApproachNotes() { return (approachNotes != null && !approachNotes.isBlank()) ? approachNotes : approach; }
    public void setApproachNotes(String approachNotes) { this.approachNotes = approachNotes; }
    public String getTimeComplexity() { return timeComplexity; }
    public void setTimeComplexity(String timeComplexity) { this.timeComplexity = timeComplexity; }
    public String getSpaceComplexity() { return spaceComplexity; }
    public void setSpaceComplexity(String spaceComplexity) { this.spaceComplexity = spaceComplexity; }
    public String getSolutionCode() { return (solutionCode != null && !solutionCode.isBlank()) ? solutionCode : codeSnippet; }
    public void setSolutionCode(String solutionCode) { this.solutionCode = solutionCode; }
    public String getCodeSnippet() { return (codeSnippet != null && !codeSnippet.isBlank()) ? codeSnippet : solutionCode; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    public String getWhatILearned() { return (whatILearned != null && !whatILearned.isBlank()) ? whatILearned : getApproach(); }
    public void setWhatILearned(String whatILearned) { this.whatILearned = whatILearned; }
    public LocalDate getSolvedDate() { return solvedDate; }
    public void setSolvedDate(LocalDate solvedDate) { this.solvedDate = solvedDate; }
}
