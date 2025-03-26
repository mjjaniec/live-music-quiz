package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.UserBadge;
import com.github.mjjaniec.model.Player;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;

import java.util.List;

public class SlackersContainer extends Div {
    public void refresh(List<Player> slackers) {
        removeAll();
        if (slackers.isEmpty()) {
            add(new H4("wszyscy odpowiedzieli!"));
        } else {
            add(new H4("czekamy na"));
            slackers.forEach(player -> add(new UserBadge(player.name(), true, true)));
        }
    }

}
