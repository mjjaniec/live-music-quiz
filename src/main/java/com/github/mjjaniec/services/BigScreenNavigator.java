package com.github.mjjaniec.services;

import com.github.mjjaniec.views.bigscreen.BigScreenRoute;

public interface BigScreenNavigator {
    <T extends BigScreenRoute> void navigateBigScreen(Class<T> view);
    void refreshPlayerLists();
    void refreshSlackersList();
    void refreshBigScreen();
}
