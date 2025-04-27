package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.components.UserBadge;
import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.util.LocalStorage;
import com.github.mjjaniec.lmq.util.Plural;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "piece-result", layout = PlayerView.class)
public class PieceResultView extends VerticalLayout implements PlayerRoute {

    private interface FakeAutocomplete {
        void set(String value, boolean isSuccess);
        Component it();
    }

    private final GameService gameService;
    private final H1 pointsHolder = new H1();
    private final H3 pointsCaptionHolder = new H3();
    private final FakeAutocomplete fakeArtist = fakeAutocomplete();
    private final FakeAutocomplete fakeTitle = fakeAutocomplete();

    public PieceResultView(GameService gameService) {
        this.gameService = gameService;
        setSpacing(true);
        setPadding(true);

        setSizeFull();
//        setAlignItems(Alignment.CENTER);
//        setJustifyContentMode(JustifyContentMode.EVENLY);

        add(new H5("artysta:"));
        add(fakeArtist.it());
        add(new H5("tytuł:"));
        add(fakeTitle.it());

        add(new H3("zdobywa"));
        add(pointsHolder);
        add(pointsCaptionHolder);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        forPlayer(attachEvent.getUI(), player -> {
            gameService.getCurrentAnswer(player).ifPresent(answer -> {
                fakeArtist.set(answer.actualArtist(), answer.artist());
                fakeTitle.set(answer.actualTitle(), answer.title());
            });
            int points = gameService.getCurrentPlayerPoints(player);
            pointsHolder.setText(String.valueOf(points));
            pointsCaptionHolder.setText(Plural.points(points));
        });
    }


    private FakeAutocomplete fakeAutocomplete() {
        Input input = new Input();
        input.setEnabled(false);
        input.setWidthFull();
        Div div = new Div(input);
        div.addClassName("autoComplete_wrapper");
        div.setWidthFull();
        return new FakeAutocomplete() {
            @Override
            public void set(String value, boolean isSuccess) {
                input.setValue(value);
                input.addClassName(isSuccess ? "success": "failure");
            }

            @Override
            public Component it() {
                return div;
            }
        };
    }
}
