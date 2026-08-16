package com.github.mjjaniec.lmq.views.maestro;

import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "maestro/feedback")
@RolesAllowed("MAESTRO")
public class FeedbackView extends VerticalLayout implements RouterLayout {

    FeedbackView(MaestroInterface gameService) {
        add(new RouterLink("Back", MaestroView.class));
        gameService.getFeedbacks().forEach(feedback -> add(new Paragraph(feedback)));
    }
}
