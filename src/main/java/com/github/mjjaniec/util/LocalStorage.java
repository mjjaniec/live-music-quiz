package com.github.mjjaniec.util;

import com.github.mjjaniec.model.Player;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.WebStorage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LocalStorage {
    private static final String USERNAME = "lmq-username";

    public static CompletableFuture<Optional<Player>> readPlayer(UI ui) {
        return WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, USERNAME)
                .thenApply(value -> Optional.ofNullable(value).map(Player::new));
    }

    public static void savePlayer(UI ui, Player player) {
        WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, USERNAME, player.name);
    }

    public static void removePlayer(UI ui) {
        WebStorage.removeItem(ui, WebStorage.Storage.LOCAL_STORAGE, USERNAME);
    }
}
