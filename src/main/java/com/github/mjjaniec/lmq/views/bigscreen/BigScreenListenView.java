package com.github.mjjaniec.lmq.views.bigscreen;

import com.github.mjjaniec.lmq.components.NotesAnimation;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "listen", layout = BigScreenView.class)
public class BigScreenListenView extends VerticalLayout implements BigScreenRoute {
    public BigScreenListenView() {
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        add(new Div());
        add(new H1("Słuchaj słuchaj jaj jaj"));
        add(new VerticalLayout());

        Component animation = new NotesAnimation();
        animation.getStyle().setMaxWidth("8em");
        add(animation);
    }
}
