package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.model.Constants;
import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.github.mjjaniec.lmq.util.TestId.testId;


@JsModule(value = "./setupAutocomplete.ts")
@Route(value = "answer", layout = PlayerView.class)
public class AnswerView extends VerticalLayout implements PlayerRoute {
    private final Input artist = testId(new Input(), "player/answer/artist");
    private final Input title = testId(new Input(), "player/answer/title");
    private final GameService gameService;
    private final Button confirm = testId(new Button("Potwierdzam"), "player/answer/confirm");
    private static final String ARTIST_PATH = "api/v1/hint/artist";
    private static final String TITLE_PATH = "api/v1/hint/title";

    private final Map<String, Boolean> isProvidedMap = new HashMap<>(Map.of(ARTIST_PATH, false, TITLE_PATH, false));

    @ClientCallable
    public void setProvided(String path, boolean set) {
        isProvidedMap.put(path, set);
        confirm.setEnabled(isProvidedMap.values().stream().allMatch(v -> v));
    }

    public AnswerView(GameService gameService) {
        this.gameService = gameService;
        gameService.pieceStage().ifPresent(piece -> {
            artist.setId("artist-input");
            title.setId("title-input");

            add(new H5("artysta:"));
            add(artist);
            add(new H5("tytuł:"));
            add(title);

            confirm.setEnabled(false);
            confirm.setWidthFull();
            confirm.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
            add(confirm);

            H4 waitMessage1 = new H4("poczekaj na pozostałych graczy");
            H3 waitMessage2 = new H3("\uD83E\uDD71 \uD83D\uDCA4 \uD83D\uDCA4");
            VerticalLayout waitLayout = new VerticalLayout(waitMessage1, waitMessage2);
            waitLayout.setAlignItems(Alignment.CENTER);
            waitLayout.setVisible(false);

            if (Constants.UNKNOWN.equals(piece.piece.artist())) {
                artist.setValue(Constants.UNKNOWN);
                artist.setEnabled(false);
                isProvidedMap.put(ARTIST_PATH, true);
            }


            confirm.addClickListener(_ -> forPlayer(UI.getCurrent(), player -> {
                boolean correctArtist = !Constants.UNKNOWN.equals(piece.piece.artist()) && isCorrect(artist.getValue(), piece.piece.artist(), piece.piece.artistAlternative());
                boolean correctTitle = isCorrect(title.getValue(), piece.piece.title(), piece.piece.titleAlternative());
                gameService.reportResult(
                        player,
                        correctArtist,
                        correctTitle,
                        artist.getValue(),
                        title.getValue()
                );
                artist.setEnabled(false);
                title.setEnabled(false);
                confirm.setVisible(false);
                waitLayout.setVisible(true);
            }));

            add(confirm);
            add(waitLayout);
        });
    }

    private boolean isCorrect(@Nullable String value, String correct, @Nullable String correctAlternative) {
        return value != null && (value.equals(correct) || value.equals(correctAlternative));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setupAutocomplete(artist, "Podaj artystę...", ARTIST_PATH);
        setupAutocomplete(title, "Podaj tytuł...", TITLE_PATH);
        forPlayer(attachEvent.getUI(), player -> gameService.getCurrentAnswer(player).ifPresent(answer -> {
            artist.setValue(answer.actualArtist());
            title.setValue(answer.actualTitle());
            artist.addClassName("wait");
            title.addClassName("wait");
            confirm.click();
        }));
    }

    private void setupAutocomplete(Input input, String placeholder, String sourcePath) {
        input.getElement().executeJs("window.setupAutocomplete($0, $1, $2, $3, $4)",
                input.getId().orElse(""),
                placeholder,
                sourcePath,
                "🤷 Nie wiem 🤷",
                getElement());
        input.setValueChangeMode(ValueChangeMode.ON_BLUR);
        input.setWidthFull();
    }
}
