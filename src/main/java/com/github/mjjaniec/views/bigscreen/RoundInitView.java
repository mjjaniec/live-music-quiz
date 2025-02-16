package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.ProgressBar;
import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;

import java.util.Optional;


@Route(value = "round-init", layout = BigScreenView.class)
public class RoundInitView extends VerticalLayout implements BigScreenRoute {

    public RoundInitView(GameService gameService) {
        setSpacing(false);
        setPadding(false);
        setSizeFull();
        getStyle().setBackgroundColor(Palette.GREEN);
        setJustifyContentMode(JustifyContentMode.BETWEEN);

        gameService.stage().asRoundInit()
                .or(this::testInit)   //temporary
                .ifPresent(this::setupUI);
    }

    @Override
    public Optional<GameStage.RoundPiece> testPiece() {
        return Optional.empty();
    }

    private void setupUI(GameStage.RoundInit roundInit) {
        add(new Div());
        add(keyValue("Witajcie w rundzie", String.valueOf(roundInit.roundNumber().number())));
        add(keyValue("Kto odpowiada", roundInit.difficulty().mode == MainSet.RoundMode.EVERYBODY ? "Wszyscy" : "Pierwsza/y" ));
        add(keyValue("Punkty za wykonawcę", String.valueOf(roundInit.difficulty().points.artist())));
        add(keyValue("Punkty za tytuł",  String.valueOf(roundInit.difficulty().points.title())));
        add(keyValue("Przed nami utworów", String.valueOf(roundInit.pieces().size())));
        add(new Div());
        add(new Div());
    }

    private HorizontalLayout keyValue(String caption, String value) {
        HorizontalLayout result = new HorizontalLayout();
        result.getThemeList().add("spacing-ls");
        result.setWidthFull();
        H1 left = new H1(caption + ": ");
        H1 right = new H1(value);

        left.getStyle().setTextAlign(Style.TextAlign.RIGHT);
        left.getStyle().setColor(Palette.LIGHTER);
        left.getStyle().setFontSize("5em");

        right.getStyle().setColor(Palette.WHITE).setFontWeight(Style.FontWeight.BOLD);
        right.getStyle().setFontSize("5em");

        result.add(left, new Div(), right);
        result.setFlexGrow(5, left);
        result.setFlexGrow(5, right);
        return result;
    }
}
