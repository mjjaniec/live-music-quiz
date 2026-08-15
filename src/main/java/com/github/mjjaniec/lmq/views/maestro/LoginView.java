package com.github.mjjaniec.lmq.views.maestro;

import static com.github.mjjaniec.lmq.util.TestId.testId;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;

@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        boolean sent = event.getLocation().getQueryParameters().getParameters().containsKey("sent");
        if (sent) {
            add(testId(
                    new Paragraph("Sprawdź swoją skrzynkę e-mail — wysłaliśmy Ci link do zalogowania."),
                    "maestro/login/sent"));
            return;
        }
        add(new H2("Zaloguj się jako prowadzący"));
        add(requestLinkForm(VaadinServletRequest.getCurrent()));
    }

    private Html requestLinkForm(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String csrfInput = csrfToken != null
                ? "<input type=\"hidden\" name=\"%s\" value=\"%s\"/>"
                        .formatted(csrfToken.getParameterName(), csrfToken.getToken())
                : "";
        String html = """
                <form method="post" action="%s/ott/generate">
                  <label for="username">Adres e-mail</label>
                  <input type="email" id="username" name="username" data-testid="maestro/login/email" required autofocus />
                  %s
                  <button type="submit" data-testid="maestro/login/submit">Wyślij link logowania</button>
                </form>
                """.formatted(request.getContextPath(), csrfInput);
        return new Html(html);
    }
}
