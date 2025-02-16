package com.github.mjjaniec.views.bigscreen;

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

import java.util.List;
import java.util.Optional;



@Route(value = "round-init", layout = BigScreenView.class)
public class RoundInitView extends VerticalLayout implements BigScreenRoute {


    // todo remove
    private Optional<GameStage.RoundInit> testInit() {
        return Optional.of(new GameStage.RoundInit(new GameStage.RoundNumber(1,3),
                MainSet.Difficulty.Easy,
                List.of(),
                new GameStage.RoundSummary(new GameStage.RoundNumber(1,3))
        ));
    }

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

    private void setupUI(GameStage.RoundInit roundInit) {
        VerticalLayout progress = new VerticalLayout();

        progress.add(createProgress(roundInit.roundNumber().number(), roundInit.roundNumber().of(), Palette.DARKER));
        progress.getStyle().setBackground(Palette.GREEN_DARKER);
        progress.setPadding(false);
        progress.setSpacing(false);
        add(progress);


        add(keyValue("Witajcie w rundzie", String.valueOf(roundInit.roundNumber().number())));
        add(keyValue("Kto odpowiada", roundInit.difficulty().mode == MainSet.RoundMode.EVERYBODY ? "Wszyscy" : "Pierwsza/y" ));
        add(keyValue("Punkty za wykonawcę", String.valueOf(roundInit.difficulty().points.artist())));
        add(keyValue("Punkty za tytuł",  String.valueOf(roundInit.difficulty().points.title())));
        add(keyValue("Przed nami utworów", String.valueOf(roundInit.pieces().size())));
        add(new Div());
    }

    private HorizontalLayout keyValue(String caption, String value) {
        HorizontalLayout result = new HorizontalLayout();
        result.setWidthFull();
        result.getStyle().setColor(Palette.WHITE);
        H1 left = new H1(caption + ":");
        H1 right = new H1(value);
        left.setWidth("60%");
        left.getStyle().setTextAlign(Style.TextAlign.RIGHT);
        left.getStyle().setColor(Palette.LIGHTER);
        right.setWidth("40%");
        right.getStyle().setColor(Palette.WHITE);

        result.add(left, right);
        return result;
    }


    private HorizontalLayout createProgress(long step, long of, String color) {
        HorizontalLayout result = new HorizontalLayout();
        result.getStyle().setColor(Palette.WHITE);
        result.setSpacing(false);
        result.setPadding(false);
        result.setWidthFull();
        VerticalLayout left = new VerticalLayout();
        left.setPadding(false);
        left.getStyle().setPaddingLeft("1em").setPaddingTop("0.1em");
        long leftW = step * 100 / of;
        left.setWidth(leftW + "%");
        left.setHeight("2em");
        left.add(new Text("Runda: " + " " + step + " / " + of));
        Div right = new Div();
        left.getStyle().setBackgroundColor(color);
        right.setWidth((100 - leftW) + "%");


        result.add(left, right);

        return result;
    }
}
