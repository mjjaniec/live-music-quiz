package com.github.mjjaniec.views.player;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Cookies;
import com.github.mjjaniec.util.R;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route(value = R.Player.Join.PATH, layout = PlayerView.class)
public class JoinView extends VerticalLayout {

    public JoinView(GameService service) {
        setSpacing(true);
        add(new Div(new Text("Welcome to Live Music Quiz by Michał Janiec!")));
        add(new Div(new Text("Enter you name and join:")));
        TextField field = new TextField("name");
        Button join = new Button("Join");
        join.setEnabled(false);
        join.addClickListener(event -> {
            if (service.addPlayer(field.getValue())) {
                Cookies.savePlayer(new Player(field.getValue()));
                UI.getCurrent().navigate("player/wait");
            } else {
                field.setInvalid(true);
                field.setErrorMessage("the name is already occupied");
            }
        });
        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.addInputListener(event -> join.setEnabled(!field.getValue().isBlank()));
        field.setWidthFull();
        join.setWidthFull();
        join.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(field);
        add(join);
    }
}
