package com.github.mjjaniec.services;

import com.github.mjjaniec.views.player.PlayerRoute;
import com.vaadin.flow.component.Component;

public interface PlayerNavigator {
    <T extends Component & PlayerRoute> void navigatePlayers(Class<T> view);
    void refreshAllPlayers();
}
