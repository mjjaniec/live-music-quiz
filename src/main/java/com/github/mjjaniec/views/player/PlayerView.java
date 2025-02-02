package com.github.mjjaniec.views.player;

import com.github.mjjaniec.services.PlayerService;
import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

import java.util.Objects;

@Route(value = "player")
@RoutePrefix(value = "player")
public class PlayerView extends VerticalLayout implements RouterLayout {


    private final HorizontalLayout outlet = new HorizontalLayout();


    @Override
    public void showRouterLayoutContent(HasElement content) {
        if (content != null) {
            outlet.getElement().appendChild(Objects.requireNonNull(content.getElement()));
        }
    }

    public PlayerView() {
        setPadding(false);
        setSpacing(false);
        Image banner = new Image("themes/live-music-quiz/banner.svg", "banner");
        banner.setSizeFull();
        setSizeFull();
        HorizontalLayout header = new HorizontalLayout(banner);
        header.setPadding(true);
        header.getStyle().setBackground(Palete.BLUE);
        header.setHeight("12em");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        outlet.setSizeFull();


        Text footerText = new Text("by Michał Janiec");

        VerticalLayout footer = new VerticalLayout();
        footer.setHeight("4em");
        footer.setPadding(true);
        footer.getStyle().setBackground(Palete.BLUE);
        footer.getStyle().setColor(Palete.WHITE);
        footer.setAlignItems(Alignment.END);
        footer.setWidthFull();
        footer.add(footerText);


        add(header);
        add(outlet);
        add(footer);

        // temporary:
        setWidth("360px");
        setHeight("700px");
    }
}