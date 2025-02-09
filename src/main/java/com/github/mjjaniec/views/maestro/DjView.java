package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.BigScreenNavigator;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.services.PlayerNavigator;
import com.github.mjjaniec.util.Palete;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.github.mjjaniec.views.player.JoinView;
import com.google.common.collect.Streams;
import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.Rendering;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

import java.util.Optional;

@Route(value = "dj", layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {

    private final GameService gameService;
    private final BigScreenNavigator bigScreenNavigator;
    private final PlayerNavigator playerNavigator;

    DjView(GameService gameService, BigScreenNavigator bigScreenNavigator, PlayerNavigator playerNavigator) {
        this.gameService = gameService;
        this.bigScreenNavigator = bigScreenNavigator;
        this.playerNavigator = playerNavigator;
        setSizeFull();

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
        Span header = new Span("\uD83D\uDCF2 Zaproszenie");
        VerticalLayout main = new VerticalLayout();
        HorizontalLayout buttons = new HorizontalLayout(
                new Button("Aktywuj", event -> {
                    bigScreenNavigator.navigateBigScreen(InviteView.class);
                    playerNavigator.navigatePlayers(JoinView.class);
                }),
                new RouterLink("BigScreen", InviteView.class),
                new RouterLink("Player Join", JoinView.class)
        );
        buttons.setAlignItems(Alignment.CENTER);
        main.add(buttons);
        main.add(playersList());
        return new AccordionPanel(header, main);
    }

    private Component playersList() {
        Grid<Player> grid = new Grid<>(Player.class, false);
        grid.addColumn(Player::name).setHeader("Ksywka");
        grid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player -> {
            Div result = new Div();
            Checkbox danger = new Checkbox("danger", false);
            Button bumpOut = new Button("Wyrzuć", event -> gameService.removePlayer(player));
            bumpOut.addThemeVariants(ButtonVariant.LUMO_ERROR);
            bumpOut.setEnabled(false);
            danger.addValueChangeListener(event -> bumpOut.setEnabled(event.getValue()));
            result.add(danger, bumpOut);
            return result;
        })).setHeader("Akcje");
        grid.setItems(gameService.getPlayers());
        return grid;
    }

    private AccordionPanel finishComponent() {
        Span header = new Span("\uD83C\uDFC6 Podsumowanie");
        HorizontalLayout content = new HorizontalLayout();
        return new AccordionPanel(header, content);
    }


    private AccordionPanel roundComponent(MainSet.LevelPieces pieces, long number) {
        Div header = new Div(new Span("\uD83C\uDFAF Runda " + (number + 1)));
        Accordion content = new Accordion();
        content.getStyle().setMarginLeft("3em");
        pieces.pieces().stream().map(this::pieceComponent).forEach(content::add);
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
}
