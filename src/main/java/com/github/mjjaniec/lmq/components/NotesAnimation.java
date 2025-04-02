package com.github.mjjaniec.lmq.components;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class NotesAnimation extends HorizontalLayout {
    public NotesAnimation() {
        setSpacing(false);
        setPadding(false);
        setWidthFull();
        getStyle().setFontSize("1.6em");
        Span note1 = new Span("\uD834\uDD95");
        Span note2 = new Span("\uD834\uDD61");
        Span note3 = new Span("\uD834\uDD61");
        Span note4 = new Span("\uD834\uDD95");
        note1.setClassName("note1");
        note2.setClassName("note2");
        note3.setClassName("note3");
        note4.setClassName("note4");

        getStyle().setFontSize("6em").setLineHeight("1");
        add(note1, note2, note3, note4);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.EVENLY);
    }
}
