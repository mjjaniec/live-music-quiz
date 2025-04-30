package com.github.mjjaniec.lmq.views.bigscreen;

import com.github.mjjaniec.lmq.components.UserBadge;
import com.github.mjjaniec.lmq.model.Player;
import com.github.mjjaniec.lmq.util.Palette;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.dom.Style;

import java.util.List;

public class SlackersContainer extends Div {
    public void refresh(List<Player> slackers) {
        removeAll();
        if (slackers.isEmpty()) {
            H3 h3 = new H3("Wszyscy odpowiedzieli!");
            h3.getStyle().setColor(Palette.GREEN);
            h3.getStyle().setMarginTop("4rem");
            h3.getStyle().setTextAlign(Style.TextAlign.CENTER);
            add(h3);
        } else {
            H4 h4 = new H4("Czekamy na:");
            h4.getStyle().setMarginBottom("1rem");
            add(h4);
            slackers.forEach(player -> add(new UserBadge(player.name(), true, true)));
        }
    }

}
