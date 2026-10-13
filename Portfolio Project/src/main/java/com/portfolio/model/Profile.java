package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The owner's profile (a single row): hero text, bio, contact links and headline stats. */
@Entity
@Table(name = "profile")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 300)
    private String tagline;

    /** Paragraphs separated by a blank line. */
    @Column(nullable = false, length = 4000)
    private String bio;

    @Column(length = 120)
    private String location;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @Column(name = "projects_completed", nullable = false)
    private int projectsCompleted;

    @Column(name = "happy_clients", nullable = false)
    private int happyClients;

    protected Profile() { }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getTitle() { return title; }
    public String getTagline() { return tagline; }
    public String getBio() { return bio; }
    public String getLocation() { return location; }
    public String getEmail() { return email; }
    public String getGithubUrl() { return githubUrl; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public int getYearsExperience() { return yearsExperience; }
    public int getProjectsCompleted() { return projectsCompleted; }
    public int getHappyClients() { return happyClients; }
}
