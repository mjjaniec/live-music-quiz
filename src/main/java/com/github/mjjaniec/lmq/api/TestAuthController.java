package com.github.mjjaniec.lmq.api;

import com.github.mjjaniec.lmq.services.MaestroUserDetailsService;
import com.github.mjjaniec.lmq.stores.MaestroStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("integration-test")
@RestController
@RequiredArgsConstructor
public class TestAuthController {

    private final MaestroStore maestroStore;
    private final MaestroUserDetailsService userDetailsService;

    @GetMapping("/test/login")
    public void login(@RequestParam String email, HttpServletRequest request) {
        maestroStore.createIfAbsent(email);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
