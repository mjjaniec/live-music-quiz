package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.util.LocalStorage;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;


@Route(value = "answer", layout = PlayerView.class)
public class AnswerView extends VerticalLayout implements PlayerRoute {
    private final Input artist = new Input();
    private final Input title = new Input();

    public AnswerView() {
        artist.setId("artist-input");
        title.setId("title-input");

        add(new Span("artysta:"));
        add(artist);
        add(new Span("tytuł:"));
        add(title);

        Button confirm = new Button("Potwierdzam");

        title.addValueChangeListener(event -> {
            System.out.println("simple value change listener: " + event.getValue());
        });

        confirm.setEnabled(false);
        confirm.setWidthFull();
        confirm.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
        add(confirm);

        confirm.addClickListener(event -> {
            UI ui = UI.getCurrent();
            LocalStorage.readPlayer(ui).thenAccept(playerOpt -> playerOpt.ifPresent(player -> {
//                gameService.reportResult(player, artist, title, bonus ? 2 : 1);
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


    private void setupAutocompleteOld(Input input, String json) {
        input.getElement().executeJs("const element = new autoComplete(" + json + " ); element.input.setAttribute('autocomplete', 'off');");
        input.setWidthFull();
    }

    private void setupAutocomplete(Input input, String placeholder, String sourcePath) {
        String js = """
                const config = {
                    selector: "#<element-id>",
                    placeHolder: "<place-holder>",
                    data: {
                         cache: true,
                         src: async (query) => {
                              try {
                                const source = await fetch("<api-path>");
                                return await source.json();
                              } catch (error) {
                                return error;
                              }
                         }
                    },
                    resultItem: {
                        highlight: true
                    },
                    diacritics: true,
                    events: {
                        input: {
                            selection: (event) => {
                                const selection = event.detail.selection.value;
                                element.input.value = selection;
                                element.input.blur();
                            }
                        }
                    }
                };
                const element = new autoComplete(config);
                element.input.setAttribute("autocomplete", "off");
                """;
        input.getElement().executeJs(js
                .replace("<element-id>", input.getId().orElse(""))
                .replace("<place-holder>", placeholder)
                .replace("<api-path>", sourcePath));
        input.setWidthFull();
    }

}
