package com.portfolio.service;

import com.portfolio.dto.Dtos.ProjectRequest;
import com.portfolio.dto.Dtos.ProjectResponse;
import com.portfolio.exception.ResourceNotFoundException;
import com.portfolio.model.Project;
import com.portfolio.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Write side for projects (admin only). */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public ProjectResponse create(ProjectRequest r) {
        Project p = new Project(r.title().strip(), r.summary().strip(), r.techStack().strip(),
                blankToNull(r.repoUrl()), blankToNull(r.liveUrl()), r.featured(), r.displayOrder());
        return ProjectResponse.from(repository.save(p));
    }

    public ProjectResponse update(Long id, ProjectRequest r) {
        Project p = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
        p.update(r.title().strip(), r.summary().strip(), r.techStack().strip(),
                blankToNull(r.repoUrl()), blankToNull(r.liveUrl()), r.featured(), r.displayOrder());
        return ProjectResponse.from(p);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }
        repository.deleteById(id);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }
}
