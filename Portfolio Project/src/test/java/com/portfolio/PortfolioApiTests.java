package com.portfolio;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.repository.ContactMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

/**
 * End-to-end API tests against the full application context (H2 + Flyway + real security chain).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "portfolio.admin.username=admin",
        "portfolio.admin.password=test-password-123"
})
class PortfolioApiTests {

    private static final String VALID_CONTACT = """
            {"name":"Jane","email":"jane@example.com","subject":"Hi","message":"Hello there, nice portfolio!","website":""}""";

    private static final String VALID_PROJECT = """
            {"title":"New","summary":"Summary","techStack":"Java,Spring","repoUrl":"https://example.com/r",
             "liveUrl":"","featured":false,"displayOrder":50}""";

    @Autowired WebApplicationContext context;
    @Autowired ContactMessageRepository messages;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    // ---------- Public read API ----------

    @Test
    void profileIsPublicAndSplitsBioIntoParagraphs() throws Exception {
        mvc().perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").isNotEmpty())
                .andExpect(jsonPath("$.bio", hasSize(3)));
    }

    @Test
    void skillsProjectsAndTestimonialsAreServed() throws Exception {
        mvc().perform(get("/api/skills")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(12)));
        mvc().perform(get("/api/projects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].techStack").isArray());
        mvc().perform(get("/api/testimonials")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void securityHeadersAreSent() throws Exception {
        mvc().perform(get("/api/profile"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    // ---------- Contact form ----------

    @Test
    void contactRequiresCsrfToken() throws Exception {
        mvc().perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(VALID_CONTACT))
                .andExpect(status().isForbidden());
    }

    @Test
    void validContactIsStored() throws Exception {
        long before = messages.count();
        mvc().perform(post("/api/contact").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_CONTACT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("received")));
        org.junit.jupiter.api.Assertions.assertEquals(before + 1, messages.count());
    }

    @Test
    void invalidContactReturnsFieldErrorsWithoutInternals() throws Exception {
        String bad = """
                {"name":"","email":"not-an-email","subject":"","message":"short"}""";
        mvc().perform(post("/api/contact").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void honeypotSubmissionIsSilentlyDropped() throws Exception {
        long before = messages.count();
        String bot = """
                {"name":"Bot","email":"bot@example.com","subject":"Buy","message":"Spam spam spam spam","website":"http://spam"}""";
        mvc().perform(post("/api/contact").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bot))
                .andExpect(status().isCreated());
        org.junit.jupiter.api.Assertions.assertEquals(before, messages.count());
    }

    @Test
    void malformedJsonGivesGeneric400() throws Exception {
        mvc().perform(post("/api/contact").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Authorisation ----------

    @Test
    void adminEndpointsRejectAnonymousUsers() throws Exception {
        mvc().perform(get("/api/admin/messages")).andExpect(status().isUnauthorized());
        mvc().perform(post("/api/admin/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROJECT)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsRejectNonAdminRoles() throws Exception {
        mvc().perform(get("/api/admin/messages").with(user("bob").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void unknownPathsAreDeniedByDefault() throws Exception {
        mvc().perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc().perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginSucceedsWithConfiguredCredentialsAndFailsOtherwise() throws Exception {
        mvc().perform(post("/api/auth/login").with(csrf()).param("username", "admin").param("password", "test-password-123"))
                .andExpect(status().isOk());
        mvc().perform(post("/api/auth/login").with(csrf()).param("username", "admin").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Admin CRUD ----------

    @Test
    void adminCanCreateUpdateAndDeleteProjects() throws Exception {
        var created = mvc().perform(post("/api/admin/projects").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PROJECT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.techStack", hasSize(2)))
                .andReturn();
        String body = created.getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc().perform(put("/api/admin/projects/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PROJECT.replace("\"New\"", "\"Renamed\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title", is("Renamed")));

        mvc().perform(delete("/api/admin/projects/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc().perform(delete("/api/admin/projects/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void projectUrlsMustBeHttp() throws Exception {
        String evil = VALID_PROJECT.replace("https://example.com/r", "javascript:alert(1)");
        mvc().perform(post("/api/admin/projects").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(evil))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.repoUrl").exists());
    }
}
