package com.github.mjjaniec.lmq.components;

import com.vaadin.flow.component.html.Span;

public class UserBadge extends Span {

    public UserBadge(String user, boolean small, boolean bigScreen) {
        super(user);
        String fontSize = small ? "2em" : "2.5em";
        if (bigScreen) {
            String margin = small ? "0.4em" : "0.5em";
            getStyle().setMarginRight(margin);
            getStyle().setMarginBottom(margin);

        }
        getStyle().setFontSize(fontSize);
        getElement().getThemeList().add("badge pill");
        if (bigScreen) {
            getElement().getThemeList().add("success");
        }
    }
}
