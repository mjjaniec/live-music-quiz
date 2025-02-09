package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.BannerBand;
import com.github.mjjaniec.components.FooterBand;
import com.github.mjjaniec.components.RouterLayoutWithOutlet;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;


@Route("")
@RoutePrefix("big-screen")
public class BigScreenView extends VerticalLayout implements RouterLayoutWithOutlet<VerticalLayout>, BigScreenRoute {
    private final VerticalLayout outlet = new VerticalLayout();
    private final BroadcastAttach broadcaster;

    @Override
    public VerticalLayout outlet() {
        return outlet;
    }

    public BigScreenView(BroadcastAttach broadcaster) {
        this.broadcaster = broadcaster;
        outlet.setSizeFull();
        outlet.setPadding(false);
        outlet.getStyle().setBackgroundColor(Palete.WHITE);
        setPadding(true);
        setSpacing(false);
        getStyle().setBackground(Palete.GREEN);
        setSizeFull();
        add(new BannerBand(Palete.GREEN));
        add(outlet);
        add(new FooterBand(Palete.GREEN));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcaster.attachBigScreenUI(attachEvent.getUI());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcaster.detachBigScreenUI(detachEvent.getUI());
        super.onDetach(detachEvent);
    }
}
