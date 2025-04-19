package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.util.LocalStorage;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;


@JsModule(value = "./setupAutocomplete.ts")
@Route(value = "answer", layout = PlayerView.class)
public class AnswerView extends VerticalLayout implements PlayerRoute {
    private final Input artist = new Input();
    private final Input title = new Input();

    private boolean artistSet = false, titleSet = false;

    public AnswerView() {
        artist.setId("artist-input");
        title.setId("title-input");

        add(new H5("artysta:"));
        add(artist);
        add(new H5("tytuł:"));
        add(title);

        Button confirm = new Button("Potwierdzam");

        artist.addValueChangeListener(event -> {
            artistSet = true;
            if (titleSet) confirm.setEnabled(true);
        });

        title.addValueChangeListener(event -> {
            titleSet = true;
            if (artistSet) confirm.setEnabled(true);
        });

        confirm.setEnabled(false);
        confirm.setWidthFull();
        confirm.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
        add(confirm);

        confirm.addClickListener(event -> {
            UI ui = UI.getCurrent();
            LocalStorage.readPlayer(ui).thenAccept(playerOpt -> playerOpt.ifPresent(player -> {
                ui.access(() -> ui.navigate(PieceResultView.class));
            }));
        });

        add(confirm);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setupAutocomplete(artist, "Podaj artystę...", "api/v1/hint/artist");
        setupAutocomplete(title, "Podaj tytuł...", "api/v1/hint/title");
    }

    private void setupAutocomplete(Input input, String placeholder, String sourcePath) {
        input.getElement().executeJs("window.setupAutocomplete($0, $1, $2, $3)",
                input.getId().orElse(""),
                placeholder,
                sourcePath,
                "🤷 Nie wiem 🤷");
        input.setValueChangeMode(ValueChangeMode.ON_BLUR);
        input.setWidthFull();
    }
}
