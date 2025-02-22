package com.github.mjjaniec.views.player;

import com.github.mjjaniec.components.UserBadge;
import com.github.mjjaniec.util.LocalStorage;
import com.github.mjjaniec.util.Plural;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "piece-result", layout = PlayerView.class)
public class PieceResultView extends VerticalLayout implements PlayerRoute {

    private final Div badgeHolder = new Div();

    public PieceResultView() {
        setSpacing(true);
        setPadding(true);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.EVENLY);
        add(badgeHolder);
        add(new H3("zdobywa"));
        add(new H1("12"));
        add(new H3(Plural.points(12)));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        LocalStorage.readPlayer(ui).thenAccept(opt -> opt.ifPresent(player -> ui.access(() -> {
            badgeHolder.removeAll();
            badgeHolder.add(new UserBadge(player.name, false, false));
        })));
    }
}
