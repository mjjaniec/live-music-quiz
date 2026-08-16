package com.github.mjjaniec.lmq.services;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class MagicLinkEmailSuccessHandler implements OneTimeTokenGenerationSuccessHandler {

    private final MagicLinkOneTimeTokenService tokenService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken)
            throws IOException {
        if (!tokenService.isSuppressed(oneTimeToken)) {
            dispatch(request, oneTimeToken);
        }
        response.sendRedirect(request.getContextPath() + "/maestro/login?sent=true");
    }

    private void dispatch(HttpServletRequest request, OneTimeToken oneTimeToken) {
        String magicLink = UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .port(request.getServerPort())
                .path(request.getContextPath())
                .path("/login/ott")
                .queryParam("token", oneTimeToken.getTokenValue())
                .toUriString();

        if (!StringUtils.hasText(smtpHost)) {
            log.info("SMTP not configured — magic link for {}: {}", oneTimeToken.getUsername(), magicLink);
            return;
        }

        sendEmail(oneTimeToken.getUsername(), magicLink);
    }

    private void sendEmail(String email, String magicLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("Twój link logowania — Live Music Quiz");
            helper.setText(plainTextBody(magicLink), htmlBody(magicLink));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send magic link email to {}", email, e);
        }
    }

    private static String plainTextBody(String magicLink) {
        return """
                Kliknij poniższy link, aby zalogować się do Live Music Quiz:

                %s

                Link jest ważny przez 30 minut i można go użyć tylko raz.
                Jeśli to nie Ty, zignoruj tę wiadomość.
                """.formatted(magicLink);
    }

    private static String htmlBody(String magicLink) {
        return """
                <p>Kliknij poniższy przycisk, aby zalogować się do Live Music Quiz:</p>
                <p><a href="%s" style="display:inline-block;padding:10px 20px;background:#1676f3;color:#fff;
                text-decoration:none;border-radius:4px;">Zaloguj się</a></p>
                <p>Jeśli przycisk nie działa, skopiuj ten adres do przeglądarki:<br>%s</p>
                <p>Link jest ważny przez 30 minut i można go użyć tylko raz.<br>
                Jeśli to nie Ty, zignoruj tę wiadomość.</p>
                """.formatted(magicLink, magicLink);
    }
}
