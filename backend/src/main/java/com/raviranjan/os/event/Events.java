package com.raviranjan.os.event;

public final class Events {
    private Events() {}

    public static class GitHubContributionEvent extends DomainEvent {
        public GitHubContributionEvent(Object source, String title, String message, String actionUrl) {
            super(source, DomainEventType.GITHUB_CONTRIBUTION_CHANGED, title, message, "LOW", "GITHUB", null, actionUrl);
        }
    }

    public static class NewGitHubPrEvent extends DomainEvent {
        public NewGitHubPrEvent(Object source, String title, String message, String prId, String actionUrl) {
            super(source, DomainEventType.NEW_GITHUB_PR, title, message, "MEDIUM", "GITHUB_PR", prId, actionUrl);
        }
    }

    public static class NewGitHubIssueEvent extends DomainEvent {
        public NewGitHubIssueEvent(Object source, String title, String message, String issueId, String actionUrl) {
            super(source, DomainEventType.NEW_GITHUB_ISSUE, title, message, "LOW", "GITHUB_ISSUE", issueId, actionUrl);
        }
    }

    public static class ProjectUpdatedEvent extends DomainEvent {
        public ProjectUpdatedEvent(Object source, String title, String message, String projectId, String actionUrl) {
            super(source, DomainEventType.NEW_PROJECT_UPDATE, title, message, "MEDIUM", "PROJECT", projectId, actionUrl);
        }
    }

    public static class MilestoneCompletedEvent extends DomainEvent {
        public MilestoneCompletedEvent(Object source, String title, String message, String milestoneId, String actionUrl) {
            super(source, DomainEventType.MILESTONE_COMPLETED, title, message, "HIGH", "MILESTONE", milestoneId, actionUrl);
        }
    }

    public static class ResearchPublishedEvent extends DomainEvent {
        public ResearchPublishedEvent(Object source, String title, String message, String researchId, String actionUrl) {
            super(source, DomainEventType.NEW_RESEARCH_PUBLISHED, title, message, "MEDIUM", "RESEARCH", researchId, actionUrl);
        }
    }

    public static class ArticlePublishedEvent extends DomainEvent {
        public ArticlePublishedEvent(Object source, String title, String message, String articleId, String actionUrl) {
            super(source, DomainEventType.NEW_ARTICLE_PUBLISHED, title, message, "MEDIUM", "ARTICLE", articleId, actionUrl);
        }
    }

    public static class ContactInquiryEvent extends DomainEvent {
        public ContactInquiryEvent(Object source, String title, String message, String inquiryId, String actionUrl) {
            super(source, DomainEventType.NEW_CONTACT_INQUIRY, title, message, "URGENT", "CONTACT_INQUIRY", inquiryId, actionUrl);
        }
    }

    public static class IntegrationFailedEvent extends DomainEvent {
        public IntegrationFailedEvent(Object source, String provider, String errorMessage, String actionUrl) {
            super(source, DomainEventType.INTEGRATION_FAILED, "Integration Failed: " + provider, errorMessage, "URGENT", "INTEGRATION", provider, actionUrl);
        }
    }

    public static class SynchronizationCompletedEvent extends DomainEvent {
        public SynchronizationCompletedEvent(Object source, String provider, String message, String actionUrl) {
            super(source, DomainEventType.SYNCHRONIZATION_COMPLETED, "Sync Completed: " + provider, message, "LOW", "INTEGRATION", provider, actionUrl);
        }
    }
}
