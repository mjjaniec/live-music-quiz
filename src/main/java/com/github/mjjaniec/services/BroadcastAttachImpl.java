package com.github.mjjaniec.services;

import com.github.mjjaniec.views.bigscreen.BigScreenRoute;
import com.github.mjjaniec.views.player.PlayerRoute;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Component
public class BroadcastAttachImpl implements BroadcastAttach, BigScreenNavigator, PlayerNavigator {

    private final List<UI> playerUIs = new ArrayList<>();
    private final List<UI> bigScreenUIs = new ArrayList<>();
    private final Map<UI, Runnable> playerLists = new HashMap<>();

    @Override
    public void attachPlayerUI(UI ui) {
        playerUIs.add(ui);
    }

    @Override
    public void detachPlayerUI(UI ui) {
        playerUIs.remove(ui);
    }

    @Override
    public void attachBigScreenUI(UI ui) {
        bigScreenUIs.add(ui);
    }

    @Override
    public void detachBigScreenUI(UI ui) {
        bigScreenUIs.remove(ui);
    }

    @Override
    public void attachPlayerList(UI ui, Runnable refresh) {
        playerLists.put(ui, refresh);
    }

    @Override
    public void detachPlayerList(UI ui) {
        playerLists.remove(ui);
    }

    @Override
    @SuppressWarnings("unchecked")
    public  <T extends BigScreenRoute> void navigateBigScreen(Class<T> view) {
        bigScreenUIs.forEach(ui -> ui.access(() -> ui.navigate((Class<? extends Component>)view)));
    }

    @Override
    public void refreshPlayerLists() {
        playerLists.forEach((ui, runnable) -> ui.access(runnable::run));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlayerRoute> void navigatePlayers(Class<T> view) {
        playerUIs.forEach(ui -> ui.access(() -> ui.navigate((Class<? extends Component>)view)));
    }

    @Override
    public void refreshAllPlayers() {
        List.copyOf(playerUIs).forEach(ui -> ui.access(() -> ui.refreshCurrentRoute(true)));
    }

    @Override
    public void refreshBigScreen() {
        bigScreenUIs.forEach(ui -> ui.access(() -> ui.refreshCurrentRoute(true)));
    }
}
