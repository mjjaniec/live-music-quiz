package com.github.mjjaniec.lmq.views.bigscreen;

import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.model.MainSet;
import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.services.TestDataProvider;
import com.github.mjjaniec.lmq.util.Palette;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;


@Route(value = "round-init", layout = BigScreenView.class)
public class RoundInitView extends VerticalLayout implements BigScreenRoute {

    public RoundInitView(GameService gameService, TestDataProvider testDataProvider) {
        setSpacing(false);
        setPadding(false);
        setSizeFull();
        getStyle().setBackgroundColor(Palette.GREEN);
        setJustifyContentMode(JustifyContentMode.BETWEEN);

        gameService.roundInitStage()
                .or(testDataProvider::init)   //temporary
                .ifPresent(this::setupUI);
    }

    private void setupUI(GameStage.RoundInit roundInit) {
        add(new Div());
        add(keyValue("Witajcie w rundzie", String.valueOf(roundInit.roundNumber().number())));
        add(keyValue("Kto odpowiada", whoAnswers(roundInit.roundMode())));
        add(keyValue("Punkty za wykonawcę", points(roundInit.roundMode(), roundInit.roundMode().artistPoints)));
        add(keyValue("Punkty za tytuł", points(roundInit.roundMode(), roundInit.roundMode().titlePoints)));
        add(keyValue("Przed nami utworów", String.valueOf(roundInit.pieces().size())));
        add(new Div());
        add(new Div());
    }

    private String points(MainSet.RoundMode mode, int points) {
        return switch (mode) {
            case EVERYBODY -> String.valueOf(points);
            case ONION -> 4 * points + " / " + 3 * points + " / " + 2 * points + " / " + points;
            case FIRST -> points + " / " + 2 * points + " / " + 3 * points + " / ...";
        };
    }

    private String whoAnswers(MainSet.RoundMode mode) {
        return mode == MainSet.RoundMode.FIRST ? "Pierwsza/y" : "Wszyscy";
    }

    private HorizontalLayout keyValue(String caption, String value) {
        HorizontalLayout result = new HorizontalLayout();
        result.getThemeList().add("spacing-ls");
        result.setWidthFull();
        H1 left = new H1(caption + ": ");
        H1 right = new H1(value);

        left.getStyle().setTextAlign(Style.TextAlign.RIGHT);
        left.getStyle().setColor(Palette.LIGHTER);

        right.getStyle().setColor(Palette.WHITE).setFontWeight(Style.FontWeight.BOLD);

        result.add(left, new Div(), right);
        result.setFlexGrow(5, left);
        result.setFlexGrow(5, right);
        return result;
    }
}
