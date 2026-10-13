package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A showcased project. Editable through the admin API. */
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String summary;

    /** Comma separated technology names, e.g. "Java,Spring Boot,PostgreSQL". */
    @Column(name = "tech_stack", nullable = false, length = 300)
    private String techStack;

    @Column(name = "repo_url", length = 300)
    private String repoUrl;

    @Column(name = "live_url", length = 300)
    private String liveUrl;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Project() { }

    public Project(String title, String summary, String techStack, String repoUrl, String liveUrl,
                   boolean featured, int displayOrder) {
        update(title, summary, techStack, repoUrl, liveUrl, featured, displayOrder);
    }

    /** Single mutation point used by create and update flows. */
    public void update(String title, String summary, String techStack, String repoUrl, String liveUrl,
                       boolean featured, int displayOrder) {
        this.title = title;
        this.summary = summary;
        this.techStack = techStack;
        this.repoUrl = repoUrl;
        this.liveUrl = liveUrl;
        this.featured = featured;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getTechStack() { return techStack; }
    public String getRepoUrl() { return repoUrl; }
    public String getLiveUrl() { return liveUrl; }
    public boolean isFeatured() { return featured; }
    public int getDisplayOrder() { return displayOrder; }
}
