package com.github.mjjaniec.views.player;

import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;

@Route(value = "wait-for-round", layout = PlayerView.class)
public class WaitForRoundView extends HorizontalLayout implements PlayerRoute {


    public WaitForRoundView(GameService gameService) {
        setSpacing(false);
        setPadding(false);
        setSizeFull();
        getStyle().setColor(Palete.WHITE).setFontSize("1.6em");
        Div outlet = new Div();
        gameService.stage().asRoundInit()
                .map(round -> String.valueOf(round.roundNumber().number()))
                .ifPresent(outlet::setText);
        outlet.getStyle().setFontSize("7em").setFontWeight(Style.FontWeight.BOLD).setLineHeight("1.5");
        outlet.setClassName("pulse pt-mono-regular");
        getStyle().setBackground(Palete.BLUE);
        setAlignItems(Alignment.CENTER);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setWidthFull();
        content.setAlignItems(Alignment.CENTER);
        content.add(new Div(new Text("Zaczekaj na start")));
        content.add(outlet);
        content.add(new Div(new Text("rundy!")));
        add(content);
    }
}
