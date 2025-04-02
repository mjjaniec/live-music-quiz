package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.components.UserBadge;
import com.github.mjjaniec.lmq.util.LocalStorage;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "wait-for-others", layout = PlayerView.class)
public class WaitForOthersView extends VerticalLayout implements PlayerRoute {

    private final Div badgeHolder = new Div();

    public WaitForOthersView() {
        setSpacing(true);
        setPadding(true);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        add(new H4("witaj"));
        add(badgeHolder);
        add(new H4("poczekaj na pozostałych graczy"));
        add(new H2("\uD83E\uDD71 \uD83D\uDCA4 \uD83D\uDCA4"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        LocalStorage.readPlayer(ui).thenAccept(opt -> opt.ifPresent(player -> ui.access(() -> {
            badgeHolder.removeAll();
            badgeHolder.add(new UserBadge(player.name(), false, false));
        })));
    }
}
