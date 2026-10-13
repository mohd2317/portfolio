package com.portfolio.dto;

import com.portfolio.model.ContactMessage;
import com.portfolio.model.Profile;
import com.portfolio.model.Project;
import com.portfolio.model.Skill;
import com.portfolio.model.Testimonial;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * API contracts. Entities are never serialised directly, so the persistence
 * model can evolve without breaking clients and nothing is exposed by accident.
 */
public final class Dtos {

    private Dtos() { }

    // ---------- Responses (public) ----------

    public record ProfileResponse(String fullName, String title, String tagline, List<String> bio,
                                  String location, String email, String githubUrl, String linkedinUrl,
                                  int yearsExperience, int projectsCompleted, int happyClients) {
        public static ProfileResponse from(Profile p) {
            // Paragraphs are stored separated by a blank line.
            List<String> paragraphs = Arrays.stream(p.getBio().split("\\R\\s*\\R")).map(String::strip).toList();
            return new ProfileResponse(p.getFullName(), p.getTitle(), p.getTagline(), paragraphs,
                    p.getLocation(), p.getEmail(), p.getGithubUrl(), p.getLinkedinUrl(),
                    p.getYearsExperience(), p.getProjectsCompleted(), p.getHappyClients());
        }
    }

    public record SkillResponse(Long id, String name, String category, int level) {
        public static SkillResponse from(Skill s) {
            return new SkillResponse(s.getId(), s.getName(), s.getCategory(), s.getLevel());
        }
    }

    public record ProjectResponse(Long id, String title, String summary, List<String> techStack,
                                  String repoUrl, String liveUrl, boolean featured, int displayOrder) {
        public static ProjectResponse from(Project p) {
            List<String> tech = Arrays.stream(p.getTechStack().split(",")).map(String::strip)
                    .filter(s -> !s.isEmpty()).toList();
            return new ProjectResponse(p.getId(), p.getTitle(), p.getSummary(), tech,
                    p.getRepoUrl(), p.getLiveUrl(), p.isFeatured(), p.getDisplayOrder());
        }
    }

    public record TestimonialResponse(Long id, String author, String role, String quote) {
        public static TestimonialResponse from(Testimonial t) {
            return new TestimonialResponse(t.getId(), t.getAuthor(), t.getRole(), t.getQuote());
        }
    }

    // ---------- Contact form ----------

    /**
     * Public contact form payload. {@code website} is a honeypot: it is hidden from
     * humans, so any value means a bot and the message is silently dropped.
     */
    public record ContactRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 150) String subject,
            @NotBlank @Size(min = 10, max = 3000) String message,
            @Size(max = 200) String website) {
    }

    public record ContactMessageResponse(Long id, String name, String email, String subject,
                                         String message, Instant createdAt, boolean read) {
        public static ContactMessageResponse from(ContactMessage m) {
            return new ContactMessageResponse(m.getId(), m.getName(), m.getEmail(), m.getSubject(),
                    m.getMessage(), m.getCreatedAt(), m.isRead());
        }
    }

    // ---------- Admin ----------

    /**
     * Only http(s) links are accepted so a stored value can never become a
     * {@code javascript:} URL when rendered as a link.
     */
    public record ProjectRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 1000) String summary,
            @NotBlank @Size(max = 300) String techStack,
            @Size(max = 300) @Pattern(regexp = "^(https?://\\S+)?$", message = "must be an http(s) URL") String repoUrl,
            @Size(max = 300) @Pattern(regexp = "^(https?://\\S+)?$", message = "must be an http(s) URL") String liveUrl,
            boolean featured,
            @Min(0) @Max(10_000) int displayOrder) {
    }

    public record LoginInfo(String username) { }
}
