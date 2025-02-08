package com.github.mjjaniec.views.player;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.util.Cookies;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

@Route("/")
public class RootView extends HorizontalLayout {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Cookies.readPlayer().ifPresentOrElse(
                player -> UI.getCurrent().navigate("/player/wait"),
                () -> UI.getCurrent().navigate("/player/join"));
    }
}
