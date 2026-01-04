package com.github.mjjaniec.lmq.views.bigscreen;

import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.services.TestDataProvider;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Optional;


@Route(value = "reveal", layout = BigScreenView.class)
public class RevealView extends VerticalLayout implements BigScreenRoute {

    public RevealView(GameService gameService, TestDataProvider testDataProvider) {
        setSpacing(false);
        setPadding(true);

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.EVENLY);
        setSizeFull();

        gameService.pieceStage().or(testDataProvider::piece)
                .ifPresent(piece -> {
                    String artist = piece.piece.artist() + (Optional.ofNullable(piece.piece.artistAlternative()).map(a -> " / " + a).orElse(""));
                    String title = piece.piece.title() + (Optional.ofNullable(piece.piece.titleAlternative()).map(a -> " / " + a).orElse(""));
                    add(new VerticalLayout());
                    add(new H4("artysta:"));
                    add(new H1(artist));
                    add(new VerticalLayout());
                    add(new H4("tytuł:"));
                    add(new H1(title));
                    add(new VerticalLayout());
                    add(new VerticalLayout());
                });
    }
}
