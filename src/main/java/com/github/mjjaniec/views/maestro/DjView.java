package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Palete;
import com.github.mjjaniec.util.R;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.google.common.collect.Streams;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

import java.util.Optional;

@Route(value = R.Maestro.DJ.PATH, layout = MaestroView.class)
public class DjView extends VerticalLayout implements RouterLayout {


    DjView(GameService gameService) {
        setSizeFull();
        add(new RouterLink("BigScreen", InviteView.class));
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
            Streams.mapWithIndex(quiz.levels().stream(), this::roundComponent).forEach(main::add);
            add(main);
            add(reset);
        }
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
