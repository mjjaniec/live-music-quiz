package com.github.mjjaniec.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class BannerBand extends HorizontalLayout {
    public BannerBand(String color) {
        Image banner = new Image("themes/live-music-quiz/banner.svg", "banner");
        banner.setSizeFull();
        add(banner);
        setSizeFull();
        setPadding(true);
        getStyle().setBackground(color);
        setHeight("12em");
        setWidthFull();
        setAlignItems(Alignment.CENTER);
    }
}
