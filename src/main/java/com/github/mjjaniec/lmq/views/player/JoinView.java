package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.components.BannerBand;
import com.github.mjjaniec.lmq.components.FooterBand;
import com.github.mjjaniec.lmq.config.ApplicationConfig;
import com.github.mjjaniec.lmq.model.Player;
import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.util.LocalStorage;
import com.github.mjjaniec.lmq.util.Palette;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import static com.github.mjjaniec.lmq.util.TestId.testId;

@Route(value = "player/join")
public class JoinView extends VerticalLayout {

    private final GameService service;
    private final ApplicationConfig config;

    public JoinView(GameService service, ApplicationConfig config) {
        this.service = service;
        this.config = config;
        VerticalLayout outlet = new VerticalLayout();
        setPadding(false);
        setSpacing(false);
        outlet.setSizeFull();
        setSizeFull();

        add(new BannerBand(Palette.BLUE));
        add(outlet);
        add(new FooterBand(Palette.BLUE));

        outlet.setSpacing(true);
        outlet.add(new Div(new Text("Witaj w Live Music Quiz by Michał Janiec!")));
        outlet.add(new Div(new Text("podaj ksywkę żeby dołączyć:")));
        TextField field = new TextField("ksywka");
        Button join = testId(new Button("Dołączam!"), "player/join/button");
        join.setEnabled(false);
        join.addClickListener(event -> {
            if (service.addPlayer(field.getValue())) {
                LocalStorage.savePlayer(UI.getCurrent(), new Player(field.getValue()));
                UI.getCurrent().navigate(PlayerView.class);
            } else {
                field.setInvalid(true);
                field.setErrorMessage("kswyka zajęta, wybierz inną");
            }
        });
        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.addInputListener(event -> join.setEnabled(!field.getValue().isBlank()));
        field.setWidthFull();
        join.setWidthFull();
        join.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        outlet.add(field);
        outlet.add(join);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (config.enableFrontRouting()) {
            UI ui = attachEvent.getUI();
            LocalStorage.readPlayer(ui).thenAccept(playerOpt -> playerOpt
                    .filter(service::hasPlayer)
                    .ifPresent(player -> ui.access(() -> ui.navigate(PlayerView.class)))
            );
        }
    }
}
