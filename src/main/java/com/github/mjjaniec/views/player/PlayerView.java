package com.github.mjjaniec.views.player;

import com.github.mjjaniec.components.BannerBand;
import com.github.mjjaniec.components.FooterBand;
import com.github.mjjaniec.components.RouterLayoutWithOutlet;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.LocalStorage;
import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.AttachEvent;
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

    @Override
    public HorizontalLayout outlet() {
        return outlet;
    }

    public PlayerView(BroadcastAttach broadcaster, GameService gameService) {
        this.broadcaster = broadcaster;
        this.gameService = gameService;
        setPadding(false);
        setSpacing(false);
        outlet.setSizeFull();
        setSizeFull();

        add(new BannerBand(Palete.BLUE));
        add(outlet);
        add(new FooterBand(Palete.BLUE));

        // temporary:
        setWidth("360px");
        setHeight("700px");
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

    private void kickOutOrDirect(UI ui) {
        LocalStorage.readPlayer(ui).thenAccept(playerOpt -> playerOpt.ifPresentOrElse(
                player -> {
                    if (gameService.hasPlayer(player)) {
                        ui.access(() -> Optional.ofNullable(gameService.stage()).ifPresentOrElse(
                                stage -> ui.navigate(stage.playerView()),
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