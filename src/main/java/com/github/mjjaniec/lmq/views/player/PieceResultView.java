package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.model.Constants;
import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.util.Plural;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "piece-result", layout = PlayerView.class)
public class PieceResultView extends VerticalLayout implements PlayerRoute {

    private interface FakeAutocomplete {
        void set(String value, boolean isSuccess, boolean ignore);

        Component it();
    }

    private final GameService gameService;
    private final H1 pointsHolder = new H1();
    private final H3 pointsCaptionHolder = new H3();
    private final FakeAutocomplete fakeArtist = fakeAutocomplete();
    private final FakeAutocomplete fakeTitle = fakeAutocomplete();
    private final Boolean withAutocomplete;

    public PieceResultView(GameService gameService) {
        this.gameService = gameService;
        withAutocomplete = gameService.pieceStage()
                .filter(p -> !p.innerStages.contains(GameStage.PieceStage.PLAY)).isPresent();
        setSpacing(true);
        setPadding(true);

        setSizeFull();

        if (withAutocomplete) {
            add(new H5("artysta:"));
            add(fakeArtist.it());
            add(new H5("tytuł:"));
            add(fakeTitle.it());
        }

        VerticalLayout summary = new VerticalLayout();
        summary.setPadding(false);
        summary.setAlignItems(Alignment.CENTER);

        summary.add(new H3("zdobywasz"));
        summary.add(pointsHolder);
        summary.add(pointsCaptionHolder);

        add(summary);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        forPlayer(attachEvent.getUI(), player -> {
            if (withAutocomplete) {
                gameService.getCurrentAnswer(player).ifPresent(answer -> {
                    fakeArtist.set(answer.actualArtist(), answer.artist(), Constants.UNKNOWN.equals(answer.actualArtist()));
                    fakeTitle.set(answer.actualTitle(), answer.title(), false);
                });
            }
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
            public void set(String value, boolean isSuccess, boolean ignore) {
                input.setValue(value);
                if (!ignore) {
                    input.addClassName(isSuccess ? "success" : "failure");
                }
            }

            @Override
            public Component it() {
                return div;
            }
        };
    }
}
