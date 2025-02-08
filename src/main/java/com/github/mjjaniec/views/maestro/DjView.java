package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.R;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

@Route(value = R.Maestro.DJ.PATH, layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {


    DjView(GameService gameService) {
        add(new Paragraph("dj view"));
        add(new Paragraph(gameService.quiz().toString()));

    }
}
