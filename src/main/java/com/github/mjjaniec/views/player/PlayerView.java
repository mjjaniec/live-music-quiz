package com.github.mjjaniec.views.player;

import com.github.mjjaniec.components.BannerBand;
import com.github.mjjaniec.components.FooterBand;
import com.github.mjjaniec.components.RouterLayoutWithOutlet;
import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;

@Route(value = "player")
@RoutePrefix(value = "player")
public class PlayerView extends VerticalLayout implements RouterLayoutWithOutlet<HorizontalLayout>, PlayerRoute {

    private final HorizontalLayout outlet = new HorizontalLayout();

    @Override
    public HorizontalLayout outlet() {
        return outlet;
    }

    public PlayerView() {
        setPadding(false);
        setSpacing(false);
        outlet.setSizeFull();
        setSizeFull();

        add(new BannerBand(Palete.BLUE));
        add(outlet);
        add(new FooterBand(Palete.BLUE));

        // temporary:
//        setWidth("360px");
//        setHeight("700px");
    }
}