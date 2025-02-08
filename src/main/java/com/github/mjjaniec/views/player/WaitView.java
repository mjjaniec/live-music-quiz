package com.github.mjjaniec.views.player;

import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Palete;
import com.github.mjjaniec.views.bigscreen.BigScreenView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "wait", layout = PlayerView.class)
public class WaitView extends HorizontalLayout {

    private final GameService gameService;
    private final BroadcastAttach broadcaster;

    public WaitView(GameService gameService, BroadcastAttach broadcaster) {
        this.gameService = gameService;
        this.broadcaster = broadcaster;
        setSpacing(false);
        setPadding(false);
        setSizeFull();
        getStyle().setColor(Palete.WHITE).setFontSize("1.6em");
        Div outlet = new Div();
//        outlet.setText(String.valueOf(gameService.currentLevel().round()));
        outlet.getStyle().setFontSize("10em").setFontWeight(Style.FontWeight.BOLD).setLineHeight("1");
        outlet.setClassName("pulse pt-mono-regular");
        getStyle().setBackground(Palete.BLUE);
        setAlignItems(Alignment.CENTER);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setWidthFull();
        content.setAlignItems(Alignment.CENTER);
        content.add(new Div(new Text("Please wait for round")));
        content.add(outlet);
        content.add(new Div(new Text("to start")));
        add(content);

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcaster.attachPlayerUI(attachEvent.getUI());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        broadcaster.detachPlayerUI(detachEvent.getUI());
    }
}
