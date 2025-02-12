package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.BigScreenNavigator;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.MaestroInterface;
import com.github.mjjaniec.services.PlayerNavigator;
import com.github.mjjaniec.util.Palete;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.github.mjjaniec.views.player.JoinView;
import com.google.common.collect.Streams;
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

import java.util.Optional;

@Route(value = "dj", layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {

    private final MaestroInterface gameService;
    private final BroadcastAttach broadcastAttach;
    private final Grid<Player> playersGrid = new Grid<>(Player.class, false);
    private final GameStage.Invite invite = new GameStage.Invite();

    DjView(MaestroInterface gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
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
            main.add(inviteComponent());
            Streams.mapWithIndex(quiz.levels().stream(), this::roundComponent).forEach(main::add);
            main.add(finishComponent());
            add(main);
            add(reset);
        }
    }

    private AccordionPanel inviteComponent() {
        Span header = panelHeader("\uD83D\uDCF2 Zaproszenie", invite);
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        HorizontalLayout buttons = new HorizontalLayout(
                activateComponent(invite),
                new RouterLink("BigScreen", InviteView.class),
                new RouterLink("Player Join", JoinView.class)
        );
        buttons.setAlignItems(Alignment.CENTER);
        main.add(buttons);
        main.add(playersList());
        return new AccordionPanel(header, main);
    }

    private Span panelHeader(String text, GameStage<?, ?> expectedStage) {
        if (gameService.stage().equals(expectedStage)) {
            Span result = new Span(text  + " ⭐");
            result.getStyle().setBackgroundColor(Palete.HIGHLIGHT).setColor(Palete.BLACK);
            result.getElement().getThemeList().add("badge success pill");
            return result;
        } else {
            return new Span(text);
        }
    }

    private Component activateComponent(GameStage<?, ?> expectedStage) {
        if (gameService.stage().equals(expectedStage)) {
            Span badge = new Span("Aktywne ✨⭐\uD83D\uDD25");
            badge.getElement().getThemeList().add("badge success pill");
            return badge;
        } else {
            Button button = new Button("Aktywuj", event -> {
                gameService.setStage(expectedStage);
            });
            button.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return button;
        }
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

    private AccordionPanel finishComponent() {
        Span header = new Span("\uD83C\uDFC6 Podsumowanie");
        HorizontalLayout content = new HorizontalLayout();
        return new AccordionPanel(header, content);
    }

    private AccordionPanel roundInitComponent(MainSet.Difficulty difficulty) {
        Span header = new Span("▶️ Rozpoczęcie rundy");
        HorizontalLayout content = new HorizontalLayout();
        content.add(activateComponent(new GameStage.RoundInit(new GameStage.RoundNumber(1,3), difficulty.mode)));
        content.add(new Span("typ: " + difficulty.mode + ", za-artystę: " + difficulty.points.artist() + ", za-tytuł: " + difficulty.points.title()));
        return new AccordionPanel(header, content);
    }

    private AccordionPanel roundSummaryComponent() {
        Span header = new Span("\uD83D\uDCC8 podsumowanie rundy");
        HorizontalLayout content = new HorizontalLayout();
        return new AccordionPanel(header, content);
    }


    private AccordionPanel roundComponent(MainSet.LevelPieces pieces, long number) {
        Div header = new Div(new Span("\uD83C\uDFAF Runda " + (number + 1)));
        Accordion content = new Accordion();
        content.getStyle().setMarginLeft("3em");
        content.add(roundInitComponent(pieces.level()));
        pieces.pieces().stream().map(this::pieceComponent).forEach(content::add);
        content.add(roundSummaryComponent());
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

    private AccordionPanel pieceComponent(MainSet.Piece piece) {
        Div header = new Div(icon(piece.instrument()), new Span(" " + piece.artist() + " - " + piece.title()));
        HorizontalLayout content = new HorizontalLayout();
        content.setWidthFull();
        Div instr = new Div(icon(piece.instrument()), new Span(" " + piece.instrument().name()));
        instr.setWidth("10%");
        Span tempo = new Span("\uD83E\uDD41 " + Optional.ofNullable(piece.tempo()).map(Object::toString).orElse("zmienne"));
        tempo.setWidth("10%");
        Span hint = new Span(piece.hint());
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
