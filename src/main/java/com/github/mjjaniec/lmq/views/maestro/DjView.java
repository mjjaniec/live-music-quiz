package com.github.mjjaniec.lmq.views.maestro;

import com.github.mjjaniec.lmq.components.*;
import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.services.BroadcastAttach;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.github.mjjaniec.lmq.views.bigscreen.InviteView;
import com.github.mjjaniec.lmq.views.player.JoinView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.mjjaniec.lmq.util.TestId.testId;

@Route(value = "dj", layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {

    private final MaestroInterface gameService;
    private final BroadcastAttach broadcastAttach;
    private final SpreadsheetLoader spreadsheetLoader;
    private final Grid<Player> playersGrid = new Grid<>(Player.class, false);
    private final Map<GameStage, ActivateComponent> activateComponents = new HashMap<>();
    private final Map<GameStage, StageHeader> headers = new HashMap<>();
    private final Button reset = testId(new Button("Reset"), "maestro/reset/button");
    private Optional<StageHeader> currentParentHeader = Optional.empty();

    private final Div pieceContent = new Div();
    private final Div wrapUpContent = new Div();
    private final Div playOffContent = new Div();

    private final Audio notification = new Audio("themes/live-music-quiz/notification.mp3");


    DjView(MaestroInterface gameService, BroadcastAttach broadcastAttach, SpreadsheetLoader spreadsheetLoader) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        this.spreadsheetLoader = spreadsheetLoader;

        testId(this, "maestro/dj");

        setSizeFull();
        setPadding(false);

        reset.addClickListener(_ -> {
            gameService.reset();
            getUI().ifPresent(ui -> ui.navigate(StartGameView.class));
        });

        if (gameService.isGameStarted()) {
            Accordion main = new Accordion();
            main.setSizeFull();
            Objects.requireNonNull(gameService.stageSet()).topLevelStages().stream().map(this::createStagePanel).forEach(main::add);
            add(customMessageComponent());
            add(main);
            add(notification);
        } else {
            reset.click();
        }
    }

    private Component customMessageComponent() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        TextField message = new TextField("Wiadomość do publiczności", "jeśli ustawiona, zastępuje logo na dużym ekranie");
        testId(message, "maestro/dj/message-field");
        Button button = new Button("Wyczyść");
        testId(button, "maestro/dj/message-button");
        button.setWidth("6rem");
        message.setValueChangeMode(ValueChangeMode.EAGER);
        message.addInputListener(_ -> button.setText(message.getValue().isBlank() ? "Wyczyść" : "Ustaw"));
        button.addClickListener(_ -> {
            if (message.getValue().isBlank()) {
                gameService.clearCustomMessage();
            } else {
                gameService.setCustomMessage(message.getValue());
            }
        });
        gameService.customMessage().ifPresent(msg -> {
            button.setText("Ustaw");
            message.setValue(msg);
        });
        layout.add(message);
        layout.add(button);
        layout.setAlignItems(Alignment.END);
        layout.setVerticalComponentAlignment(Alignment.END);
        message.setWidthFull();
        return layout;
    }

    private AccordionPanel createStagePanel(GameStage stage) {
        return switch (stage) {
            case GameStage.Invite invite -> inviteComponent(invite);
            case GameStage.RoundInit roundInit -> roundComponent(roundInit);
            case GameStage.WrapUp wrapUp -> wrapUpComponent(wrapUp);
            case GameStage.RoundPiece roundPiece -> pieceComponent(roundPiece);
            case GameStage.PlayOff playOff -> playOffComponent(playOff);
            case GameStage.RoundSummary roundSummary -> roundSummaryComponent(roundSummary);
        };
    }

    private AccordionPanel inviteComponent(GameStage.Invite invite) {
        Component header = createPanelHeader(new Text("\uD83D\uDCF2 Zaproszenie"), invite);
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

    private ActivateComponent createActivateComponent(GameStage stage) {
        ActivateComponent result = new ActivateComponent(stage, gameService.stage() == stage, this::onActivate);
        activateComponents.put(stage, result);
        return result;
    }

    private void onActivate(GameStage newStage) {
        GameStage oldStage = gameService.stage();
        Optional.ofNullable(activateComponents.get(oldStage)).ifPresent(ac -> ac.setActive(false));
        Optional.ofNullable(headers.get(oldStage)).ifPresent(h -> h.setActive(false));
        gameService.setStage(newStage);
        Optional.ofNullable(activateComponents.get(newStage)).ifPresent(ac -> ac.setActive(true));
        Optional.ofNullable(headers.get(newStage)).ifPresent(h -> h.setActive(true));
        refreshWrapUpContent();
        refreshPlayOffContent();
    }


    private StageHeader createPanelHeader(Component content, @Nullable GameStage stage) {
        StageHeader result = new StageHeader(content, gameService.stage() == stage, currentParentHeader);
        if (stage != null) {
            headers.put(stage, result);
        }
        return result;
    }

    private Component playersList() {
        testId(playersGrid, "maestro/players-grid");
        playersGrid.addColumn(Player::name).setHeader("Ksywka");
        playersGrid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player -> {
            Div result = new Div();
            Checkbox danger = new Checkbox("danger", false);
            testId(danger, "mastero/players-grid/danger/" + player.name());
            Button bumpOut = new Button("Wyrzuć", _ -> gameService.removePlayer(player));
            bumpOut.addThemeVariants(ButtonVariant.LUMO_ERROR);
            bumpOut.setEnabled(false);
            testId(bumpOut, "mastero/players-grid/bump-out/" + player.name());
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

    private void refreshWrapUpContent() {
        wrapUpContent.removeAll();
        gameService.wrapUpStage().ifPresent(wrapUp -> {
            int maxShowFrom = gameService.getPlayers().size() + 1;
            if (wrapUp.getShowFrom() == null) {
                wrapUp.setShowFrom(maxShowFrom);
            }
            int showFrom = Optional.ofNullable(wrapUp.getShowFrom()).orElse(maxShowFrom);
            AtomicInteger value = new AtomicInteger(showFrom);

            var showMore = testId(new Button("poka wincyj"), "maestro/wrapup/show-more");
            var showLess = testId(new Button("poka mnij"), "maestro/wrapup/show-less");
            showMore.setEnabled(value.get() > 0);
            showLess.setEnabled(value.get() < maxShowFrom);
            showMore.addClickListener(_ -> {
                wrapUp.setShowFrom(value.decrementAndGet());
                showMore.setEnabled(value.get() > 0);
                showLess.setEnabled(value.get() < maxShowFrom);
                gameService.setStage(wrapUp);
            });
            showLess.addClickListener(_ -> {
                wrapUp.setShowFrom(value.incrementAndGet());
                showMore.setEnabled(value.get() > 0);
                showLess.setEnabled(value.get() < maxShowFrom);
                gameService.setStage(wrapUp);
            });

            wrapUpContent.add(showMore, showLess);
        });
    }

    private AccordionPanel wrapUpComponent(GameStage.WrapUp wrapUp) {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        HorizontalLayout line = new HorizontalLayout();
        line.add(createActivateComponent(wrapUp));
        Checkbox danger = testId(new Checkbox("danger"), "maestro/reset/danger");
        reset.setEnabled(false);
        danger.addValueChangeListener(event -> reset.setEnabled(event.getValue()));
        line.add(danger);
        line.add(reset);
        content.add(line);
        content.add(wrapUpContent);
        refreshWrapUpContent();

        var header = createPanelHeader(new Text("\uD83C\uDFC6 Podsumowanie"), wrapUp);
        testId(header, "maestro/dj/wrapup/header");
        return new AccordionPanel(header, content);
    }

    private AccordionPanel roundInitComponent(GameStage.RoundInit roundInit) {
        HorizontalLayout content = new HorizontalLayout();
        content.add(testId(createActivateComponent(roundInit), "maestro/dj/round-init/activate-" + roundInit.roundNumber().number()));
        MainSet.RoundMode roundMode = roundInit.roundMode();
        content.add(new Span("typ: " + roundMode + ", za-artystę: " + roundMode.artistPoints + ", za-tytuł: " + roundMode.titlePoints));
        return new AccordionPanel(createPanelHeader(new Text("▶️ Rozpoczęcie rundy"), roundInit), content);
    }

    private void refreshPlayOffContent() {
        var stageSet = gameService.stageSet();
        if (stageSet == null) {
            return;
        }
        playOffContent.removeAll();
        PlayOffs playOffs = spreadsheetLoader.loadPlayOffs();

        GameStage.PlayOff playOff = stageSet.playOff();
        HorizontalLayout row = new HorizontalLayout();
        ComboBox<PlayOffs.PlayOff> comboBox = new ComboBox<>("Wybierz dogrywkę", playOffs.playOffs());
        gameService.playOffTask().ifPresent(comboBox::setValue);
        comboBox.setWidthFull();
        testId(comboBox, "maestro/dj/play-off/selection");
        comboBox.setEnabled(gameService.playOffTask().isEmpty());

        ActivateComponent activateComponent = new ActivateComponent(playOff, gameService.stage() == playOff, _ -> onActivate(playOff));
        testId(activateComponent, "maestro/dj/play-off/activate");
        Button select = testId(new Button("wybierz"), "maestro/dj/play-off/select-task");
        select.addClickListener(_ -> {
            gameService.setPlayOffTask(comboBox.getValue());
            refreshPlayOffContent();
        });
        select.setEnabled(comboBox.getValue() != null && gameService.playOffTask().isEmpty());

        activateComponents.put(playOff, activateComponent);
        activateComponent.setEnabled(comboBox.getValue() != null);
        comboBox.addValueChangeListener(event -> select.setEnabled(event.getValue() != null));
        row.add(comboBox, select);
        row.setAlignItems(Alignment.END);
        row.setWidthFull();
        playOffContent.add(activateComponent);
        playOffContent.add(row);

        if (gameService.stage() == playOff) {

            Checkbox danger = new Checkbox("danger");
            Button reset = new Button("resetuj dogrywkę");
            reset.addClickListener(_ -> {
                gameService.clearPlayOffTask();
                playOff.setPerformed(false);
                gameService.setStage(playOff);
                refreshPlayOffContent();
            });
            reset.setEnabled(false);
            danger.addValueChangeListener(event -> reset.setEnabled(event.getValue()));
            Button collectAnswers = testId(new Button("≙ Niech odpowiadajo!"), "maestro/dj/play-off/collect-answers");
            collectAnswers.setEnabled(!playOff.isPerformed() && gameService.playOffTask().isPresent());
            collectAnswers.addClickListener(_ -> {
                playOff.setPerformed(true);
                gameService.setStage(playOff);
                refreshPlayOffContent();
            });
            HorizontalLayout buttons = new HorizontalLayout(danger, reset, collectAnswers);
            buttons.setPadding(false);
            playOffContent.add(buttons);
            if (playOff.isPerformed()) {
                var slackers = gameService.getSlackers();
                playOffContent.add(new LittleSlackerList(slackers));
                if (slackers.isEmpty()) {
                    notification.play();
                }
            }
        }
    }

    private AccordionPanel playOffComponent(GameStage.PlayOff playOff) {
        StageHeader header = testId(createPanelHeader(new Text("\uD83C\uDFB2 Dogrywka"), playOff), "maestro/dj/play-off/header");

        playOffContent.setWidthFull();
        VerticalLayout content = new VerticalLayout();
        content.add(playOffContent);
        refreshPlayOffContent();
        return testId(new AccordionPanel(header, content), "maestro/dj/play-off/panel");
    }

    private AccordionPanel roundSummaryComponent(GameStage.RoundSummary roundSummary) {
        StageHeader header = testId(createPanelHeader(new Text("\uD83D\uDCC8 podsumowanie rundy"), roundSummary), "maestro/dj/round-summary-header-" + roundSummary.roundNumber().number());
        Component content;
        if (roundSummary.roundNumber().number() == roundSummary.roundNumber().of()) {
            content = new Paragraph("Użyj globalnego podsumowania");
        } else {
            content = testId(createActivateComponent(roundSummary), "maestro/dj/round-summary-activate-" + roundSummary.roundNumber().number());
        }
        return new AccordionPanel(header, content);
    }


    private AccordionPanel roundComponent(GameStage.RoundInit roundInit) {
        StageHeader header = testId(createPanelHeader(new Text("\uD83C\uDFAF Runda " + roundInit.roundNumber().number()), null), "maestro/dj/round-header-" + roundInit.roundNumber().number());
        currentParentHeader = Optional.of(header);
        Accordion content = new Accordion();
        content.getStyle().setMarginLeft("3em");
        content.add(roundInitComponent(roundInit));
        roundInit.pieces().stream().map(this::createStagePanel).forEach(content::add);
        content.add(createStagePanel(roundInit.roundSummary()));
        currentParentHeader = Optional.empty();
        GameStage stage = gameService.stage();
        header.setActive(stage == roundInit || stage == roundInit.roundSummary() ||
                         (stage instanceof GameStage.RoundPiece rp && roundInit.pieces().contains(rp)));
        return new AccordionPanel(header, content);
    }

    private AccordionPanel pieceComponent(GameStage.RoundPiece piece) {
        String pieceNumberSuffix = piece.roundNumber + "-" + piece.pieceNumber.number();
        Div headerComponent = new Div(
                new Span("\uD83C\uDFBC "),
                testId(new Span(piece.piece.artist()), "maestro/dj/piece-artist-" + pieceNumberSuffix),
                new Span(" - "),
                testId(new Span(piece.piece.title()), "maestro/dj/piece-title-" + pieceNumberSuffix));
        StageHeader header = testId(createPanelHeader(headerComponent, piece), "maestro/dj/piece-header-" + pieceNumberSuffix);
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        HorizontalLayout row = new HorizontalLayout();
        row.setPadding(false);
        row.setWidthFull();
        row.getStyle().setMarginLeft("2em");
        Span tempo = new Span("\uD83E\uDD41 " + Optional.ofNullable(piece.piece.tempo()).map(Object::toString).orElse("zmienne"));
        tempo.setWidth("10%");
        Span hint = new Span(piece.piece.hint());
        content.add(row);
        row.add(tempo, hint);
        if (gameService.stage() == piece) {
            pieceContent.removeFromParent();
            content.add(pieceContent);
            refreshPieceContent(piece);
        }
        piece.innerStages.stream()
                .map(innerStage -> new PieceStageButton(piece, innerStage, stage -> {
                    onActivate(stage);
                    pieceContent.removeFromParent();
                    content.add(pieceContent);
                    refreshPieceContent(stage);
                }))
                .forEach(row::add);

        return new AccordionPanel(header, content);
    }

    private void refreshSlackers() {
        if (gameService.stage() instanceof GameStage.RoundPiece piece) {
            if (piece.getCurrentStage() == GameStage.PieceStage.LISTEN || piece.getCurrentStage() == GameStage.PieceStage.ONION_LISTEN) {
                refreshPieceContent(piece);
            }
        } else if (gameService.stage() instanceof GameStage.PlayOff) {
            refreshPlayOffContent();
        }
    }

    private void refreshPlay() {
        if (gameService.stage() instanceof GameStage.RoundPiece piece) {
            if (piece.getCurrentStage() == GameStage.PieceStage.PLAY) {
                refreshPieceContent(piece);
            }
        }
    }


    private void refreshPieceContent(GameStage.RoundPiece piece) {
        pieceContent.removeAll();
        switch (piece.getCurrentStage()) {
            case LISTEN, ONION_LISTEN -> {
                if (piece.getCurrentStage() == GameStage.PieceStage.LISTEN) {
                    Checkbox bonus = testId(new Checkbox("Bonus", piece.isBonus()), "maestro/dj/piece-bonus");
                    bonus.addValueChangeListener(event -> {
                        piece.setBonus(event.getValue());
                        gameService.setStage(piece);
                    });
                    pieceContent.add(bonus);
                }
                List<Player> slackers = gameService.getSlackers();
                pieceContent.add(new LittleSlackerList(slackers));
                if (slackers.isEmpty()) {
                    notification.play();
                }
            }
            case REVEAL -> pieceContent.add(new LittleSlackerList(gameService.getSlackers()));
            case PLAY -> pieceContent.add(new PlayTimeComponent(piece, gameService, notification, this::refreshPlay));
        }

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachSlackersList(attachEvent.getUI(), this::refreshSlackers);
        broadcastAttach.attachPlayerList(attachEvent.getUI(), this::refreshPlayers);
        broadcastAttach.attachPlay(attachEvent.getUI(), this::refreshPlay);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        broadcastAttach.detachPlayerList(detachEvent.getUI());
        broadcastAttach.detachSlackersList(detachEvent.getUI());
        broadcastAttach.detachPlay(detachEvent.getUI());
    }
}
