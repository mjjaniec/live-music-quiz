package com.github.mjjaniec.components;

import com.github.mjjaniec.util.Palete;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;

import java.util.Optional;

public class StageHeader extends Div {
    private final Component content;
    private final Optional<StageHeader> parentHeader;

    public StageHeader(Component caption, boolean active, Optional<StageHeader> parentHeader) {
        this.content = caption;
        this.parentHeader = parentHeader;
        setWidthFull();
        setActive(active);
    }


    public void setActive(boolean active) {
        removeAll();
        if (active) {
            getStyle().setBackgroundColor(Palete.HIGHLIGHT).setColor(Palete.BLACK);
            add(content, new Text(" ⭐"));
        } else {
            getStyle().remove("background-color");
            getStyle().remove("color");
            add(content);
        }
        parentHeader.ifPresent(p -> p.setActive(active));
    }
}
