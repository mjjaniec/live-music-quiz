package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.components.BannerBand;
import com.github.mjjaniec.lmq.components.FooterBand;
import com.github.mjjaniec.lmq.components.RouterLayoutWithOutlet;
import com.github.mjjaniec.lmq.config.ApplicationConfig;
import com.github.mjjaniec.lmq.services.BroadcastAttach;
import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.util.LocalStorage;
import com.github.mjjaniec.lmq.util.Palette;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;

import java.util.Optional;

@Route(value = "", layout = RootView.class)
@RoutePrefix(value = "player")
public class PlayerView extends VerticalLayout implements RouterLayoutWithOutlet<HorizontalLayout>, PlayerRoute {

    private final HorizontalLayout outlet = new HorizontalLayout();
    private final BroadcastAttach broadcaster;
    private final GameService gameService;
    private final ApplicationConfig config;

    @Override
    public HorizontalLayout outlet() {
        return outlet;
    }

    public PlayerView(BroadcastAttach broadcaster, GameService gameService, ApplicationConfig config) {
        this.broadcaster = broadcaster;
        this.gameService = gameService;
        this.config = config;
        setPadding(false);
        setSpacing(false);
        outlet.setSizeFull();
        setSizeFull();

        add(new BannerBand(Palette.BLUE));
        add(outlet);
        add(new FooterBand(Palette.BLUE));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcaster.attachPlayerUI(attachEvent.getUI());
        kickOutOrDirect(attachEvent.getUI());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        broadcaster.detachPlayerUI(detachEvent.getUI());
    }

    @SuppressWarnings("unchecked")
    private void kickOutOrDirect(UI ui) {
        if (config.enableFrontRouting()) {
            LocalStorage.readPlayer(ui).thenAccept(playerOpt -> playerOpt.ifPresentOrElse(
                    player -> {
                        if (gameService.hasPlayer(player)) {
                            ui.access(() -> Optional.ofNullable(gameService.stage()).ifPresentOrElse(
                                    stage -> ui.navigate((Class<? extends Component>) stage.playerView()),
                                    () -> ui.navigate(WaitForOthersView.class)
                            ));
                        } else {
                            LocalStorage.removePlayer(ui);
                            ui.access(() -> ui.navigate(JoinView.class));
                        }
                    },
                    () -> ui.access(() -> ui.navigate(JoinView.class))
            ));
        }
    }

}