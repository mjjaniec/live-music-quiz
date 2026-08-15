package com.github.mjjaniec.lmq.config;

import com.github.mjjaniec.lmq.services.MagicLinkEmailSuccessHandler;
import com.github.mjjaniec.lmq.services.MagicLinkOneTimeTokenService;
import com.github.mjjaniec.lmq.views.maestro.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MagicLinkOneTimeTokenService magicLinkOneTimeTokenService,
            MagicLinkEmailSuccessHandler magicLinkEmailSuccessHandler)
            throws Exception {
        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class))
                .oneTimeTokenLogin(ott -> ott.loginProcessingUrl("/login/ott")
                        .loginPage("/login")
                        .tokenService(magicLinkOneTimeTokenService)
                        .tokenGenerationSuccessHandler(magicLinkEmailSuccessHandler)
                        .successHandler(redirectToMaestroSuccessHandler()))
                .build();
    }

    private SimpleUrlAuthenticationSuccessHandler redirectToMaestroSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler("/maestro");
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
    }
}
