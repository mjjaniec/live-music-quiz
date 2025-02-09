package com.github.mjjaniec.views.player;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;

@Route(value = "listen", layout = PlayerView.class)
public class ListenVIew extends HorizontalLayout implements PlayerRoute {


    private final HorizontalLayout outlet = new HorizontalLayout();

    public ListenVIew() {
        setSpacing(false);
        setPadding(false);
        setSizeFull();
        getStyle().setFontSize("1.6em");
        Span note1 = new Span("\uD834\uDD95");
        Span note2 = new Span("\uD834\uDD61");
        Span note3 = new Span("\uD834\uDD61");
        Span note4 = new Span("\uD834\uDD5f");
        note1.setClassName("note1");
        note2.setClassName("note2");
        note3.setClassName("note3");
        note4.setClassName("note4");

        outlet.getStyle().setFontSize("4em").setFontWeight(Style.FontWeight.BOLD).setLineHeight("1");
        outlet.add();
        outlet.add(note1, note2, note3, note4);
        outlet.getStyle().set("font-family", "monospace");
//        getStyle().setBackground(Palete.BLUE);
        setAlignItems(Alignment.CENTER);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);
        content.setWidthFull();
        content.setAlignItems(Alignment.CENTER);
        content.add(new Div(new Text("Listen")));
        content.add(outlet);
        add(content);
    }
}
