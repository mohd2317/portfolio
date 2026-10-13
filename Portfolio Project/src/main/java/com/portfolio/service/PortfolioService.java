package com.portfolio.service;

import com.portfolio.dto.Dtos.ProfileResponse;
import com.portfolio.dto.Dtos.ProjectResponse;
import com.portfolio.dto.Dtos.SkillResponse;
import com.portfolio.dto.Dtos.TestimonialResponse;
import com.portfolio.repository.ProfileRepository;
import com.portfolio.repository.ProjectRepository;
import com.portfolio.repository.SkillRepository;
import com.portfolio.repository.TestimonialRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the portfolio: everything the public site displays. */
@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final ProfileRepository profiles;
    private final SkillRepository skills;
    private final ProjectRepository projects;
    private final TestimonialRepository testimonials;

    public PortfolioService(ProfileRepository profiles, SkillRepository skills,
                            ProjectRepository projects, TestimonialRepository testimonials) {
        this.profiles = profiles;
        this.skills = skills;
        this.projects = projects;
        this.testimonials = testimonials;
    }

    public Optional<ProfileResponse> getProfile() {
        return profiles.findAll().stream().findFirst().map(ProfileResponse::from);
    }

    public List<SkillResponse> getSkills() {
        return skills.findAllByOrderByDisplayOrderAsc().stream().map(SkillResponse::from).toList();
    }

    public List<ProjectResponse> getProjects() {
        return projects.findAllByOrderByDisplayOrderAsc().stream().map(ProjectResponse::from).toList();
    }

    public List<TestimonialResponse> getTestimonials() {
        return testimonials.findAllByOrderByDisplayOrderAsc().stream().map(TestimonialResponse::from).toList();
    }
}
