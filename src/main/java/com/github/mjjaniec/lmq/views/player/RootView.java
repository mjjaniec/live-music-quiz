package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.config.ApplicationConfig;
import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.util.LocalStorage;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import lombok.RequiredArgsConstructor;

@Route("/")
@RequiredArgsConstructor
public class RootView extends HorizontalLayout implements RouterLayout {

    private final GameService gameService;
    private final ApplicationConfig config;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (config.enableFrontRouting()) {
            UI ui = attachEvent.getUI();
            LocalStorage.readPlayer(ui).thenAccept(playerOpt -> playerOpt.ifPresentOrElse(
                    player -> {
                        if (!gameService.hasPlayer(player)) {
                            LocalStorage.removePlayer(ui);
                            ui.access(() -> ui.navigate(JoinView.class));
                        } else {
                            attachEvent.getUI().navigate(PlayerView.class);
                        }
                    },
                    () -> ui.access(() -> ui.navigate(JoinView.class))
            ));
        }
    }
}
