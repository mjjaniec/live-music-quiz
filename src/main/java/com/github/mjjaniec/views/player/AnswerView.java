package com.github.mjjaniec.views.player;

import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.LocalStorage;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.Route;

import java.util.function.Consumer;


@Route(value = "answer", layout = PlayerView.class)
public class AnswerView extends VerticalLayout implements PlayerRoute {

    private Boolean artist = null;
    private Boolean title = null;
    private boolean bonus = false;

    public AnswerView(GameService gameService) {
        setSpacing(true);
        setPadding(true);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        Button confirm = new Button("Potwierdzam");

        add(new H5("Pokaż na co Cię stać!"));
        add(new Div());
        add(new H5("Wykonawca"));

        add(createRadio(x -> {
            artist = x;
            if (title != null) {
                confirm.setEnabled(true);
            }
        }));
        add(new Div());
        add(new H5("Tytył"));
        add(createRadio(x -> {
            title = x;
            if (artist != null) {
                confirm.setEnabled(true);
            }
        }));

        add(new Div());
        add(new Checkbox("bonus!", event -> {
            bonus = event.getValue();
        }));
        add(new Div());
        confirm.setEnabled(false);
        confirm.setWidthFull();
        confirm.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
        add(confirm);

        confirm.addClickListener(event -> {
            UI ui = UI.getCurrent();
            LocalStorage.readPlayer(ui).thenAccept(player -> {
                gameService.reportResult(player, artist, title, bonus);
                ui.access(() -> ui.navigate(PieceResultView.class));
            });
        });
    }


    private Component createRadio(Consumer<Boolean> onSelect) {
        HorizontalLayout result = new HorizontalLayout();
        result.addClassName("pseudo-radio");
        result.setWidthFull();

        FancyCaption missCaption = new FancyCaption("pudło");
        Button miss = new Button(missCaption);
        miss.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);

        FancyCaption hitCaption = new FancyCaption("trafione");
        Button hit = new Button(hitCaption);
        hit.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_LARGE);

        miss.addClickListener(_ -> {
            miss.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            hit.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            missCaption.setActive(true);
            hitCaption.setActive(false);
            onSelect.accept(false);
        });

        hit.addClickListener(_ -> {
            hit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            miss.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            hitCaption.setActive(true);
            missCaption.setActive(false);
            onSelect.accept(true);
        });

        result.add(miss, hit);
        result.setFlexGrow(1, miss);
        result.setFlexGrow(1, hit);
        return result;
    }


    private static class FancyCaption extends Div {
        private static final String theValue = "\u00a0";
        private final RadioButtonGroup<String> rg = new RadioButtonGroup<>(null, theValue);

        public FancyCaption(String text) {
            rg.setReadOnly(true);
            add(rg);
            add(new Text(text));
            setWidth("1em");
        }

        public void setActive(boolean active) {
            if (active) {
                rg.setValue(theValue);
            } else {
                rg.setValue(null);
            }
        }
    }


}
