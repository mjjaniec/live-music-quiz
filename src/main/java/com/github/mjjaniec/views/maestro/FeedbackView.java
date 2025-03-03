package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.services.MaestroInterface;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

@Route(value = "feedback", layout = MaestroView.class)
public class FeedbackView extends VerticalLayout implements RouterLayout {

    FeedbackView(MaestroInterface gameService) {
        gameService.getFeedbacks().forEach(feedback -> add(new Paragraph(feedback)));
    }
}
