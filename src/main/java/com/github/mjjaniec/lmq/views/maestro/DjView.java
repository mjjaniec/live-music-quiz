package com.github.mjjaniec.lmq.views.maestro;

import com.github.mjjaniec.lmq.components.*;
import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.services.BroadcastAttach;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.github.mjjaniec.lmq.views.bigscreen.InviteView;
import com.github.mjjaniec.lmq.views.player.JoinView;
import com.vaadin.flow.component.*;
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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

import java.util.*;
import java.util.stream.Collectors;

@Route(value = "dj", layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {

    private final MaestroInterface gameService;
    private final BroadcastAttach broadcastAttach;
    private final SpreadsheetLoader spreadsheetLoader;
    private final Grid<Player> playersGrid = new Grid<>(Player.class, false);
    private final Map<GameStage, ActivateComponent> activateComponents = new HashMap<>();
    private final Map<GameStage, StageHeader> headers = new HashMap<>();
    private final Button reset = new Button("Reset");
    private Optional<StageHeader> currentParentHeader = Optional.empty();

    private final Div pieceContent = new Div();
    private final Div wrapUpContent = new Div();
    private final Div playOffContent = new Div();

    private final Audio notification = new Audio("themes/live-music-quiz/notification.mp3");


    DjView(MaestroInterface gameService, BroadcastAttach broadcastAttach, SpreadsheetLoader spreadsheetLoader) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        this.spreadsheetLoader = spreadsheetLoader;

        setSizeFull();
        setPadding(false);

        reset.addClickListener(event -> {
            gameService.reset();
            getUI().ifPresent(ui -> ui.navigate(StartGameView.class));
        });

        if (gameService.isGameStarted()) {
            Accordion main = new Accordion();
            main.setSizeFull();
            gameService.stageSet().topLevelStages().stream().map(this::createStagePanel).forEach(main::add);
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
        Button button = new Button("Wyczyść");
        button.setWidth("6rem");
        message.setValueChangeMode(ValueChangeMode.EAGER);
        message.addInputListener(event -> button.setText(message.getValue().isBlank() ? "Wyczyść" : "Ustaw"));
        button.addClickListener(event -> {
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


    private StageHeader createPanelHeader(Component content, GameStage stage) {
        StageHeader result = new StageHeader(content, gameService.stage() == stage, currentParentHeader);
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

    private void refreshWrapUpContent() {
        wrapUpContent.removeAll();
        if (gameService.stage() instanceof GameStage.WrapUp wrapUp) {

            GameStage.Display minimalDisplay = gameService.minimalDisplay();
            if (wrapUp.getDisplay() == null) {
                wrapUp.setDisplay(minimalDisplay);
            }

            Map<Boolean, List<GameStage.Display>> collect = Arrays.stream(GameStage.Display.values())
                    .collect(Collectors.partitioningBy(d -> d.ordinal() < minimalDisplay.ordinal()));
            List<GameStage.Display> inactives = collect.getOrDefault(true, List.of());
            List<GameStage.Display> actives = collect.getOrDefault(false, List.of());

            if (!inactives.isEmpty()) {
                RadioButtonGroup<String> inactive = new RadioButtonGroup<>();
                inactive.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
                inactive.setLabel("Niedostępne");
                inactive.setItems(inactives.stream().map(GameStage.Display::name).toList());
                inactive.setEnabled(false);
                wrapUpContent.add(inactive);
            }

            RadioButtonGroup<String> active = new RadioButtonGroup<>();
            active.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
            active.setLabel("Co pokazać");
            active.setItems(actives.stream().map(GameStage.Display::name).toList());
            active.setValue(wrapUp.getDisplay().name());
            active.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    wrapUp.setDisplay(GameStage.Display.valueOf(event.getValue()));
                    gameService.setStage(wrapUp);
                }
            });
            wrapUpContent.add(active);
        }
    }

    private AccordionPanel wrapUpComponent(GameStage.WrapUp wrapUp) {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        HorizontalLayout line = new HorizontalLayout();
        line.add(createActivateComponent(wrapUp));
        Checkbox danger = new Checkbox("danger");
        reset.setEnabled(false);
        danger.addValueChangeListener(event -> reset.setEnabled(event.getValue()));
        line.add(danger);
        line.add(reset);
        content.add(line);
        content.add(wrapUpContent);
        refreshWrapUpContent();

        return new AccordionPanel(createPanelHeader(new Text("\uD83C\uDFC6 Podsumowanie"), wrapUp), content);
    }

    private AccordionPanel roundInitComponent(GameStage.RoundInit roundInit) {
        HorizontalLayout content = new HorizontalLayout();
        content.add(createActivateComponent(roundInit));
        MainSet.RoundMode roundMode = roundInit.roundMode();
        content.add(new Span("typ: " + (roundMode.isEverybody() ? "EVERYBODY" : "FIRST") + ", za-artystę: " + roundMode.artistPoints + ", za-tytuł: " + roundMode.titlePoints));
        return new AccordionPanel(createPanelHeader(new Text("▶️ Rozpoczęcie rundy"), roundInit), content);
    }

    private void refreshPlayOffContent() {
        playOffContent.removeAll();
        PlayOffs playOffs = spreadsheetLoader.loadPlayOffs();

        GameStage.PlayOff playOff = gameService.stageSet().playOff();
        HorizontalLayout row = new HorizontalLayout();
        ComboBox<PlayOffs.PlayOff> comboBox = new ComboBox<>("Wybierz dogrywkę", playOffs.playOffs());
        gameService.playOffTask().ifPresent(comboBox::setValue);
        comboBox.setWidthFull();
        comboBox.setEnabled(gameService.playOffTask().isEmpty());

        ActivateComponent activateComponent = new ActivateComponent(playOff, gameService.stage() == playOff, gameStage -> {
            gameService.setPlayOffTask(comboBox.getValue());
            onActivate(playOff);
        });
        activateComponents.put(playOff, activateComponent);
        activateComponent.setEnabled(comboBox.getValue() != null);
        comboBox.addValueChangeListener(event -> activateComponent.setEnabled(event.getValue() != null));
        row.add(comboBox, activateComponent);
        row.setAlignItems(Alignment.END);
        row.setWidthFull();
        playOffContent.add(row);

        if (gameService.stage() == playOff) {

            Checkbox danger = new Checkbox("danger");
            Button reset = new Button("resetuj dogrywkę");
            reset.addClickListener(event -> {
                gameService.clearPlayOffTask();
                comboBox.setValue(null);
                playOff.setPerformed(false);
                gameService.setStage(playOff);
                refreshPlayOffContent();
            });
            reset.setEnabled(false);
            danger.addValueChangeListener(event -> reset.setEnabled(event.getValue()));
            Button collectAnswers = new Button("≙ Niech odpowiadajo!");
            collectAnswers.setEnabled(!playOff.isPerformed());
            collectAnswers.addClickListener(event -> {
                playOff.setPerformed(true);
                gameService.setStage(playOff);
                refreshPlayOffContent();
            });
            HorizontalLayout buttons = new HorizontalLayout(danger, reset, collectAnswers);
            buttons.setPadding(false);
            playOffContent.add(comboBox);
            playOffContent.add(buttons);
            if (playOff.isPerformed()) {
                playOffContent.add(new LittleSlackerList(gameService.getSlackers()));
            }
        }
    }

    private AccordionPanel playOffComponent(GameStage.PlayOff playOff) {
        StageHeader header = createPanelHeader(new Text("\uD83C\uDFB2 Dogrywka"), playOff);

        playOffContent.setWidthFull();
        VerticalLayout content = new VerticalLayout();
        content.add(playOffContent);
        refreshPlayOffContent();
        return new AccordionPanel(header, content);
    }

    private AccordionPanel roundSummaryComponent(GameStage.RoundSummary roundSummary) {
        StageHeader header = createPanelHeader(new Text("\uD83D\uDCC8 podsumowanie rundy"), roundSummary);
        Component content;
        if (roundSummary.roundNumber().number() == roundSummary.roundNumber().of()) {
            content = new Paragraph("Użyj globalnego podsumowania");
        } else {
            content = createActivateComponent(roundSummary);
        }
        return new AccordionPanel(header, content);
    }


    private AccordionPanel roundComponent(GameStage.RoundInit roundInit) {
        StageHeader header = createPanelHeader(new Text("\uD83C\uDFAF Runda " + roundInit.roundNumber().number()), null);
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
        Div headerComponent = new Div(new Span(piece.piece.artist() + " - " + piece.piece.title()));
        Div header = createPanelHeader(headerComponent, piece);
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
            if (piece.getCurrentStage() == GameStage.PieceStage.LISTEN) {
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
            case LISTEN -> {
                Checkbox bonus = new Checkbox("Bonus", piece.getBonus() > 1);
                bonus.addValueChangeListener(event -> {
                    piece.setBonus(event.getValue() ? 2 : 1);
                    gameService.setStage(piece);
                });
                pieceContent.add(bonus);
                List<Player> slackers = gameService.getSlackers();
                pieceContent.add(new LittleSlackerList(slackers));
                if (slackers.isEmpty()) {
                    notification.play();
                }
            }
            case REVEAL -> pieceContent.add(new LittleSlackerList(gameService.getSlackers()));
            case PLAY -> pieceContent.add(new PlayTimeComponent(piece, gameService, notification));
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
