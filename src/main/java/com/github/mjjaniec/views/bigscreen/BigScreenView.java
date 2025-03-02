package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.BannerBand;
import com.github.mjjaniec.components.FooterBand;
import com.github.mjjaniec.components.ProgressBar;
import com.github.mjjaniec.components.RouterLayoutWithOutlet;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.services.TestDataProvider;
import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;


@Route("")
@RoutePrefix("big-screen")
public class BigScreenView extends VerticalLayout implements RouterLayoutWithOutlet<VerticalLayout>, BigScreenRoute {
    private final VerticalLayout outlet = new VerticalLayout();
    private final Div progressBarsOutlet = new Div();
    private final BroadcastAttach broadcaster;
    private final GameService gameService;
    private final TestDataProvider testDataProvider;

    @Override
    public VerticalLayout outlet() {
        return outlet;
    }

    public BigScreenView(BroadcastAttach broadcaster, GameService gameService, TestDataProvider testDataProvider) {
        this.broadcaster = broadcaster;
        this.gameService = gameService;
        this.testDataProvider = testDataProvider;

        outlet.setSizeFull();
        outlet.setPadding(false);
        outlet.getStyle().setBackgroundColor(Palette.WHITE);
        setPadding(true);
        setSpacing(false);
        getStyle().setBackground(Palette.GREEN);
        setSizeFull();

        progressBarsOutlet.setWidthFull();
        progressBarsOutlet.getStyle().setBackgroundColor(Palette.GREEN);

        Component topComponent = gameService.customMessage()
                .map(this::customMessageComponent)
                .orElse(new BannerBand(Palette.GREEN));
        add(topComponent);
        add(progressBarsOutlet);
        add(outlet);
        add(new FooterBand(Palette.GREEN));
        refreshProgressBars();
    }

    private Component customMessageComponent(String message) {
        HorizontalLayout result = new HorizontalLayout();
        result.setPadding(true);
        result.setHeight("15rem");
        result.getStyle().setBackground(Palette.GREEN);
        result.setWidthFull();
        result.setAlignItems(Alignment.CENTER);
        H1 msg = new H1(message);
        msg.getStyle().setColor(Palette.WHITE);
        result.setJustifyContentMode(JustifyContentMode.CENTER);
        result.setVerticalComponentAlignment(Alignment.CENTER);
        result.add(msg);
        return result;
    }

    private void refreshProgressBars() {
        progressBarsOutlet.removeAll();

        var roundP = gameService.stage().asRoundInit()
                .or(() -> gameService.stage().asPiece().flatMap(piece -> gameService.stageSet().roundInit(piece.roundNumber)))
                .or(() -> gameService.stage().asRoundSummary().flatMap(summary -> gameService.stageSet().roundInit(summary.roundNumber().number())))
                .or(testDataProvider::init).map(init ->
                        new ProgressBar("Runda", init.roundNumber().number(), init.roundNumber().of(), Palette.DARKER));
        var pieceP = gameService.stage().asPiece().or(testDataProvider::piece).map(piece ->
                new ProgressBar("Utwór", piece.pieceNumber.number(), piece.pieceNumber.of(), Palette.DARKER));

        roundP.ifPresent(round -> pieceP.ifPresent(piece -> {
            round.getStyle().setBorderBottom("None");
            round.getStyle().setBorderRadius("0.3em 0.3em 0 0");
            piece.getStyle().setBorderRadius("0 0 0.3em 0.3em");
        }));

        roundP.ifPresent(progressBarsOutlet::add);
        pieceP.ifPresent(progressBarsOutlet::add);

        if (progressBarsOutlet.getChildren().findFirst().isPresent()) {
            progressBarsOutlet.getStyle().setPaddingBottom("1em");
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcaster.attachBigScreenUI(attachEvent.getUI());
        broadcaster.attachProgressBar(attachEvent.getUI(), this::refreshProgressBars);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcaster.detachBigScreenUI(detachEvent.getUI());
        broadcaster.detachProgressBar(detachEvent.getUI());
        super.onDetach(detachEvent);
    }
}
