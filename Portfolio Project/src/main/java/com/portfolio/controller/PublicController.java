package com.portfolio.controller;

import com.portfolio.dto.Dtos.ContactRequest;
import com.portfolio.dto.Dtos.ProfileResponse;
import com.portfolio.dto.Dtos.ProjectResponse;
import com.portfolio.dto.Dtos.SkillResponse;
import com.portfolio.dto.Dtos.TestimonialResponse;
import com.portfolio.service.ContactService;
import com.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated API consumed by the portfolio page. */
@RestController
@RequestMapping("/api")
public class PublicController {

    private final PortfolioService portfolio;
    private final ContactService contact;

    public PublicController(PortfolioService portfolio, ContactService contact) {
        this.portfolio = portfolio;
        this.contact = contact;
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> profile() {
        return portfolio.getProfile().map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/skills")
    public List<SkillResponse> skills() {
        return portfolio.getSkills();
    }

    @GetMapping("/projects")
    public List<ProjectResponse> projects() {
        return portfolio.getProjects();
    }

    @GetMapping("/testimonials")
    public List<TestimonialResponse> testimonials() {
        return portfolio.getTestimonials();
    }

    /** Contact form. Responds 201 with a fixed body so bots learn nothing from the reply. */
    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> contact(@Valid @RequestBody ContactRequest request) {
        contact.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "received"));
    }
}
