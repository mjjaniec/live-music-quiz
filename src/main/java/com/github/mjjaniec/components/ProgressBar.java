package com.github.mjjaniec.components;

import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ProgressBar extends HorizontalLayout {

    public ProgressBar(String what, long step, long of, String color) {
        getStyle().setColor(Palette.WHITE);
        getStyle().setBorder("solid 2px #ffffffbb");
        getStyle().setBorderRadius("0.3em");
        setSpacing(false);
        setPadding(false);
        setWidthFull();
        VerticalLayout left = new VerticalLayout();
        left.setPadding(false);
        left.getStyle().setPaddingLeft("1em").setPaddingTop("0.1em");
        long leftW = step * 100 / of;
        left.setWidth(leftW + "%");
        left.setHeight("2em");
        left.getStyle().setBorderRadius("0.3em");
        left.add(new Text(what + ": " + " " + step + " / " + of));
        Div right = new Div();
        left.getStyle().setBackgroundColor(color);
        right.setWidth((100 - leftW) + "%");


        add(left, right);
    }
}
