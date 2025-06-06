package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.views.bigscreen.BigScreenRoute;
import com.github.mjjaniec.lmq.views.player.PlayerRoute;

public interface Navigator {
    <T extends BigScreenRoute> void navigateBigScreen(Class<T> view);

    void refreshPlayerLists();

    void refreshSlackersList();

    void refreshCustomMessage();

    void refreshProgressBar();

    void refreshPlay();

    void refreshWrapUp();

    <T extends PlayerRoute> void navigatePlayers(Class<T> view);

    void refreshAllPlayers();

    void refreshPlayOff();

    void refreshBonus();
}
