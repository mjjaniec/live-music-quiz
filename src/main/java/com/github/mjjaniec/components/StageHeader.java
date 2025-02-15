package com.github.mjjaniec.components;

import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.Optional;

public class StageHeader extends Div {
    private final String caption;
    private final Optional<StageHeader> parentHeader;

    public StageHeader(String caption, boolean active, Optional<StageHeader> parentHeader) {
        this.caption = caption;
        this.parentHeader = parentHeader;
        setWidthFull();
        setActive(active);
    }


    public void setActive(boolean active) {
        removeAll();
        if (active) {
            Span content  = new Span( caption + " ⭐");
            getStyle().setBackgroundColor(Palete.HIGHLIGHT).setColor(Palete.BLACK);
            add(content);
        } else {
            getStyle().remove("background-color");
            getStyle().remove("color");
            add(new Span(caption));
        }
        parentHeader.ifPresent(p -> p.setActive(active));
    }
}
