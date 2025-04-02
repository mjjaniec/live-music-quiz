package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.components.NotesAnimation;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;


@Route(value = "listen", layout = PlayerView.class)
public class ListenView extends VerticalLayout implements PlayerRoute {


    public ListenView() {
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        add(new H3("Słuchaj słuchaj jaj jaj"));
        add(new VerticalLayout());
        add(new NotesAnimation());
    }
}
