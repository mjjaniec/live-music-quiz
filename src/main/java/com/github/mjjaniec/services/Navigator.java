package com.github.mjjaniec.services;

import com.github.mjjaniec.views.bigscreen.BigScreenRoute;
import com.github.mjjaniec.views.player.PlayerRoute;

public interface Navigator {
    <T extends BigScreenRoute> void navigateBigScreen(Class<T> view);
    void refreshPlayerLists();
    void refreshSlackersList();
    void refreshBigScreen();
    void refreshProgressBar();
    void refreshPlay();

    <T extends PlayerRoute> void navigatePlayers(Class<T> view);
    void refreshAllPlayers();
}
