package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.components.ActivateComponent;
import com.github.mjjaniec.components.StageHeader;
import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.MaestroInterface;
import com.github.mjjaniec.util.Palete;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.github.mjjaniec.views.player.JoinView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

import java.util.*;

@Route(value = "dj", layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {

    private final MaestroInterface gameService;
    private final BroadcastAttach broadcastAttach;
    private final Grid<Player> playersGrid = new Grid<>(Player.class, false);
    private final Map<GameStage<?, ?>, ActivateComponent> activateComponents = new HashMap<>();
    private final Map<GameStage<?, ?>, StageHeader> headers = new HashMap<>();
    private Optional<StageHeader> currentParentHeader = Optional.empty();


    DjView(MaestroInterface gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        List<GameStage<?, ?>> allStages = gameService.allStages();

        setSizeFull();
        setPadding(false);

        Button reset = new Button("Reset");
        reset.addClickListener(event -> {
            gameService.reset();
            getUI().ifPresent(ui -> ui.navigate(StartGameView.class));
        });

        MainSet quiz = gameService.quiz();
        if (quiz == null) {
            reset.click();
        } else {
            Accordion main = new Accordion();
            main.setSizeFull();
            allStages.stream().map(this::createStagePanel).forEach(main::add);
            add(main);
            add(reset);
        }
    }

    private AccordionPanel createStagePanel(GameStage<?,?> stage) {
        return switch(stage) {
            case GameStage.Invite invite -> inviteComponent(invite);
            case GameStage.RoundInit roundInit -> roundComponent(roundInit);
            case GameStage.WrapUp wrapUp -> wrapUpComponent(wrapUp);
            case GameStage.RoundPiece roundPiece -> pieceComponent(roundPiece);
            case GameStage.RoundSummary roundSummary -> roundSummaryComponent(roundSummary);
            default -> throw new UnsupportedOperationException("unsupported stage" + stage);
        };
    }


    private AccordionPanel inviteComponent(GameStage.Invite invite) {
        Component header = createPanelHeader("\uD83D\uDCF2 Zaproszenie", invite);
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        HorizontalLayout buttons = new HorizontalLayout(
                createActivateComponent(invite),
                new RouterLink("BigScreen", InviteView.class),
                new RouterLink("Player Join", JoinView.class)
        );
        buttons.setAlignItems(Alignment.CENTER);
        main.add(buttons);
        main.add(playersList());
        return new AccordionPanel(header, main);
    }

    private ActivateComponent createActivateComponent(GameStage<?,?> stage) {
        ActivateComponent result = new ActivateComponent(stage,gameService.stage() == stage, newStage -> {
            GameStage<?, ?> oldStage = gameService.stage();
            Optional.ofNullable(activateComponents.get(oldStage)).ifPresent(ac -> ac.setActive(false));
            Optional.ofNullable(headers.get(oldStage)).ifPresent(h -> h.setActive(false));
            gameService.setStage(newStage);
            Optional.ofNullable(activateComponents.get(newStage)).ifPresent(ac -> ac.setActive(true));
            Optional.ofNullable(headers.get(newStage)).ifPresent(h -> h.setActive(true));
        });
        activateComponents.put(stage, result);
        return result;
    }


    private StageHeader createPanelHeader(String text, GameStage<?, ?> stage) {
        StageHeader result = new StageHeader(text, gameService.stage() == stage, currentParentHeader);
        if (stage != null) {
            headers.put(stage, result);
        }
        return result;
    }

    private Component playersList() {
        playersGrid.addColumn(Player::name).setHeader("Ksywka");
        playersGrid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player -> {
            Div result = new Div();
            Checkbox danger = new Checkbox("danger", false);
            Button bumpOut = new Button("Wyrzuć", event -> gameService.removePlayer(player));
            bumpOut.addThemeVariants(ButtonVariant.LUMO_ERROR);
            bumpOut.setEnabled(false);
            danger.addValueChangeListener(event -> bumpOut.setEnabled(event.getValue()));
            result.add(danger, bumpOut);
            return result;
        })).setHeader("Akcje");
        playersGrid.setItems(gameService.getPlayers());
        playersGrid.getStyle().setMarginRight("1em");
        return playersGrid;
    }

    private void refreshPlayers() {
        playersGrid.setItems(gameService.getPlayers());
    }

    private AccordionPanel wrapUpComponent(GameStage.WrapUp wrapUp) {
        HorizontalLayout content = new HorizontalLayout();
        return new AccordionPanel(createPanelHeader("\uD83C\uDFC6 Podsumowanie", wrapUp), content);
    }

    private AccordionPanel roundInitComponent(GameStage.RoundInit roundInit) {
        HorizontalLayout content = new HorizontalLayout();
        content.add(createActivateComponent(roundInit));
        MainSet.Difficulty difficulty = roundInit.difficulty();
        content.add(new Span("typ: " + difficulty.mode + ", za-artystę: " + difficulty.points.artist() + ", za-tytuł: " + difficulty.points.title()));
        return new AccordionPanel(createPanelHeader("▶️ Rozpoczęcie rundy", roundInit), content);
    }

    private AccordionPanel roundSummaryComponent(GameStage.RoundSummary roundSummary) {
        StageHeader header = createPanelHeader("\uD83D\uDCC8 podsumowanie rundy", roundSummary);
        Component content = createActivateComponent(roundSummary);
        return new AccordionPanel(header, content);
    }


    private AccordionPanel roundComponent(GameStage.RoundInit roundInit) {
        StageHeader header = createPanelHeader("\uD83C\uDFAF Runda " + roundInit.roundNumber().number(), null);
        currentParentHeader = Optional.of(header);
        Accordion content = new Accordion();
        content.getStyle().setMarginLeft("3em");
        content.add(roundInitComponent(roundInit));
        roundInit.pieces().stream().map(this::createStagePanel).forEach(content::add);
        content.add(createStagePanel(roundInit.roundSummary()));
        currentParentHeader = Optional.empty();
        header.setActive(gameService.stage() == roundInit);
        return new AccordionPanel(header, content);
    }

    private HtmlContainer icon(MainSet.Instrument instrument) {
        if (MainSet.Instrument.Bass == instrument) {
            Div result = new Div(instrument.icon);
            result.getStyle().setColor(Palete.BLUE).setFontWeight(Style.FontWeight.BOLD)
                    .setWidth("22px")
                    .setTransform("translate(3px, 3px)")
                    .setFontSize("120%")
                    .setDisplay(Style.Display.INLINE_BLOCK).setLineHeight("0").setWidth("20px");
            return result;
        } else {
            Div result = new Div(instrument.icon);
            result.getStyle().setDisplay(Style.Display.INLINE_BLOCK);
            result.setWidth("22px");
            return result;
        }
    }

    private AccordionPanel pieceComponent(GameStage.RoundPiece piece) {
        Div header = new Div(icon(piece.piece().instrument()), new Span(" " + piece.piece().artist() + " - " + piece.piece().title()));
        HorizontalLayout content = new HorizontalLayout();
        content.setWidthFull();
        Div instr = new Div(icon(piece.piece().instrument()), new Span(" " + piece.piece().instrument().name()));
        instr.setWidth("10%");
        Span tempo = new Span("\uD83E\uDD41 " + Optional.ofNullable(piece.piece().tempo()).map(Object::toString).orElse("zmienne"));
        tempo.setWidth("10%");
        Span hint = new Span(piece.piece().hint());
        Button Play = new Button("▶ Play");
        Button Guess = new Button("� Guess");
        content.add(instr, tempo, hint, Play, Guess);
        content.getStyle().setMarginLeft("2em");
        return new AccordionPanel(header, content);
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachPlayerList(attachEvent.getUI(), this::refreshPlayers);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        broadcastAttach.detachPlayerList(detachEvent.getUI());
    }
}
