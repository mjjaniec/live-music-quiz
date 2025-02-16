package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.BannerBand;
import com.github.mjjaniec.components.FooterBand;
import com.github.mjjaniec.components.ProgressBar;
import com.github.mjjaniec.components.RouterLayoutWithOutlet;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;


@Route("")
@RoutePrefix("big-screen")
public class BigScreenView extends VerticalLayout implements RouterLayoutWithOutlet<VerticalLayout>, BigScreenRoute {
    private final VerticalLayout outlet = new VerticalLayout();
    private final BroadcastAttach broadcaster;

    @Override
    public VerticalLayout outlet() {
        return outlet;
    }

    public BigScreenView(BroadcastAttach broadcaster, GameService gameService) {
        this.broadcaster = broadcaster;
        outlet.setSizeFull();
        outlet.setPadding(false);
        outlet.getStyle().setBackgroundColor(Palette.WHITE);
        setPadding(true);
        setSpacing(false);
        getStyle().setBackground(Palette.GREEN);
        setSizeFull();
        add(new BannerBand(Palette.GREEN));
        add(makeProgressBars(gameService));
        add(outlet);
        add(new FooterBand(Palette.GREEN));
    }

    private Component makeProgressBars(GameService gameService) {
        Div container = new Div();
        container.setWidthFull();
        container.getStyle().setBackgroundColor(Palette.GREEN);

        var roundP = gameService.stage().asRoundInit().or(this::testInit).map(init ->
                new ProgressBar("Runda", init.roundNumber().number(), init.roundNumber().of(), Palette.DARKER));
        var pieceP = gameService.stage().asPiece().or(this::testPiece).map(piece ->
                new ProgressBar("Utwór", piece.pieceNumber.number(), piece.pieceNumber.of(), Palette.DARKER));

        roundP.ifPresent(round -> pieceP.ifPresent(piece -> {
            round.getStyle().setBorderBottom("None");
            round.getStyle().setBorderRadius("0.3em 0.3em 0 0");
            piece.getStyle().setBorderRadius("0 0 0.3em 0.3em");
        }));

        roundP.ifPresent(container::add);
        pieceP.ifPresent(container::add);

        if (container.getChildren().findFirst().isPresent()) {
            container.getStyle().setPaddingBottom("1em");
        }
        return container;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcaster.attachBigScreenUI(attachEvent.getUI());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcaster.detachBigScreenUI(detachEvent.getUI());
        super.onDetach(detachEvent);
    }
}
