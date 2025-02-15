package com.github.mjjaniec.services;

import com.github.mjjaniec.views.player.PlayerRoute;

public interface PlayerNavigator {
    <T extends PlayerRoute> void navigatePlayers(Class<T> view);
    void refreshAllPlayers();
}
