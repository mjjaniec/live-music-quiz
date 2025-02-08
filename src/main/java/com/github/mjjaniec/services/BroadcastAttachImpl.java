package com.github.mjjaniec.services;

import com.github.mjjaniec.util.R;
import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
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
    public void navigateBigScreen(R.RI path) {
        playerUIs.forEach(ui -> ui.access(() -> ui.navigate(path.get())));
    }

    @Override
    public void refreshPlayers() {
        playerLists.forEach((ui, runnable) -> ui.access(runnable::run));
    }

    @Override
    public void navigatePlayers(R.RI path) {
        bigScreenUIs.forEach(ui -> ui.access(() -> ui.navigate(path.get())));
    }
}
