package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.model.Player;
import com.github.mjjaniec.lmq.util.LocalStorage;
import com.vaadin.flow.component.UI;

import java.util.function.Consumer;

public interface PlayerRoute {

    default void forPlayer(UI ui, Consumer<Player> action) {
        LocalStorage.readPlayer(ui).thenAccept(playerOpt -> ui.access(() -> playerOpt.ifPresent(action)));
    }
}
