package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.views.bigscreen.BigScreenRoute;
import com.github.mjjaniec.lmq.views.player.PlayerRoute;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Component
public class BroadcastAttachImpl implements BroadcastAttach, Navigator {

    private final List<UI> playerUIs = new ArrayList<>();
    private final List<UI> bigScreenUIs = new ArrayList<>();
    private final Map<UI, Runnable> playerLists = new HashMap<>();
    private final Map<UI, Runnable> slackersList = new HashMap<>();
    private final Map<UI, Runnable> progressBars = new HashMap<>();
    private final Map<UI, Runnable> plays = new HashMap<>();
    private final Map<UI, Runnable> wrapUps = new HashMap<>();
    private final Map<UI, Runnable> playOffs = new HashMap<>();

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
    public void attachSlackersList(UI ui, Runnable refresh) {
        slackersList.put(ui, refresh);
    }

    @Override
    public void detachSlackersList(UI ui) {
        slackersList.remove(ui);
    }

    @Override
    public void attachProgressBar(UI ui, Runnable refresh) {
        progressBars.put(ui, refresh);
    }

    @Override
    public void detachProgressBar(UI ui) {
        progressBars.remove(ui);
    }

    @Override
    public void attachPlay(UI ui, Runnable refresh) {
        plays.put(ui, refresh);
    }

    @Override
    public void detachPlay(UI ui) {
        plays.remove(ui);
    }

    @Override
    public void attachWrapUp(UI ui, Runnable refresh) {
        wrapUps.put(ui, refresh);
    }

    @Override
    public void detachWrapUp(UI ui) {
        wrapUps.remove(ui);
    }

    @Override
    public void attachPlayOff(UI ui, Runnable refresh) {
        playOffs.put(ui, refresh);
    }

    @Override
    public void detachPlayOff(UI ui) {
        playOffs.remove(ui);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BigScreenRoute> void navigateBigScreen(Class<T> view) {
        bigScreenUIs.forEach(ui -> ui.access(() -> ui.navigate((Class<? extends Component>) view)));
    }

    @Override
    public void refreshPlayerLists() {
        playerLists.forEach((ui, runnable) -> ui.access(runnable::run));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlayerRoute> void navigatePlayers(Class<T> view) {
        playerUIs.forEach(ui -> ui.access(() -> ui.navigate((Class<? extends Component>) view)));
    }

    @Override
    public void refreshAllPlayers() {
        List.copyOf(playerUIs).forEach(ui -> ui.access(() -> ui.refreshCurrentRoute(true)));
    }

    @Override
    public void refreshCustomMessage() {
        bigScreenUIs.forEach(ui -> ui.access(() -> ui.refreshCurrentRoute(true)));
    }

    @Override
    public void refreshSlackersList() {
        slackersList.forEach((ui, runnable) -> ui.access(runnable::run));
    }

    @Override
    public void refreshProgressBar() {
        progressBars.forEach((ui, runnable) -> ui.access(runnable::run));
    }

    @Override
    public void refreshPlay() {
        plays.forEach((ui, runnable) -> ui.access(runnable::run));
    }

    @Override
    public void refreshWrapUp() {
        wrapUps.forEach((ui, runnable) -> ui.access(runnable::run));
    }

    @Override
    public void refreshPlayOff() {
        playOffs.forEach((ui, runnable) -> ui.access(runnable::run));
    }
}
