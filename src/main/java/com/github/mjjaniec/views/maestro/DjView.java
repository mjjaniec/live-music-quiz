package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.R;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

@Route(value = R.Maestro.DJ.PATH, layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {


    DjView(GameService gameService) {
        add(new Paragraph("dj view"));
        add(new RouterLink("BigScreen", InviteView.class));
        add(new Paragraph(gameService.quiz().toString()));
    }
}
