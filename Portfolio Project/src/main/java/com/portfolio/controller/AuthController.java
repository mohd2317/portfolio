package com.portfolio.controller;

import com.portfolio.dto.Dtos.LoginInfo;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login and logout are handled by Spring Security filters (see SecurityConfig).
 * This endpoint lets the admin page discover whether a session is active:
 * 200 when authenticated, 401 otherwise.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public LoginInfo me(Principal principal) {
        return new LoginInfo(principal.getName());
    }
}
