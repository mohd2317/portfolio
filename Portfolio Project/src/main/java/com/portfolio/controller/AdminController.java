package com.portfolio.controller;

import com.portfolio.dto.Dtos.ContactMessageResponse;
import com.portfolio.dto.Dtos.ProjectRequest;
import com.portfolio.dto.Dtos.ProjectResponse;
import com.portfolio.service.ContactService;
import com.portfolio.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin API. Access (ROLE_ADMIN) is enforced for /api/admin/** in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProjectService projects;
    private final ContactService contacts;

    public AdminController(ProjectService projects, ContactService contacts) {
        this.projects = projects;
        this.contacts = contacts;
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request) {
        return projects.create(request);
    }

    @PutMapping("/projects/{id}")
    public ProjectResponse updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projects.update(id, request);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        projects.delete(id);
    }

    @GetMapping("/messages")
    public List<ContactMessageResponse> messages() {
        return contacts.list();
    }

    @PatchMapping("/messages/{id}/read")
    public ContactMessageResponse markRead(@PathVariable Long id) {
        return contacts.markRead(id);
    }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable Long id) {
        contacts.delete(id);
    }
}
