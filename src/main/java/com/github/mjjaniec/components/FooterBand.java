package com.github.mjjaniec.components;

import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class FooterBand extends VerticalLayout {

    public FooterBand(String color) {
        Text footerText = new Text("by Michał Janiec");

        setHeight("4em");
        setPadding(true);
        getStyle().setBackground(color);
        getStyle().setColor(Palette.WHITE);
        setAlignItems(FlexComponent.Alignment.END);
        setWidthFull();
        add(footerText);
    }
}
