package com.github.mjjaniec.lmq.components;

import com.github.mjjaniec.lmq.util.Palette;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Style;

import java.util.Optional;

public class StageHeader extends Div {
    private final Component content;
    private final Optional<StageHeader> parentHeader;

    public StageHeader(Component caption, boolean active, Optional<StageHeader> parentHeader) {
        this.content = caption;
        this.parentHeader = parentHeader;
        getStyle().setDisplay(Style.Display.INLINE_FLEX);
        setWidthFull();
        setActive(active);
    }


    public void setActive(boolean active) {
        removeAll();
        if (active) {
            getStyle().setBackgroundColor(Palette.HIGHLIGHT).setColor(Palette.BLACK);
            add(content, new Text(" ⭐"));
        } else {
            getStyle().remove("background-color");
            getStyle().remove("color");
            add(content);
        }
        parentHeader.ifPresent(p -> p.setActive(active));
    }
}
