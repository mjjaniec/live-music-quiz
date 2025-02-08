package com.github.mjjaniec.services;

import com.github.mjjaniec.util.R;
import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BroadcasterImpl implements Broadcaster, BigScreenNavigator, PlayerNavigator {

    private final List<UI> playerUIs = new ArrayList<>();
    private final List<UI> bigScreenUIs = new ArrayList<>();

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
    public void navigateBigScreen(R.RI path) {
        playerUIs.forEach(ui -> ui.access(() -> ui.navigate(path.get())));
    }

    @Override
    public void navigatePlayers(R.RI path) {
        bigScreenUIs.forEach(ui -> ui.access(() -> ui.navigate(path.get())));
    }
}
