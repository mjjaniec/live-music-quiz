package com.github.mjjaniec.lmq.util;

import com.vaadin.flow.component.Component;

public class TestId {
    public static <C extends Component> C testId(C component, String testId) {
        component.getElement().setAttribute("data-testid", testId);
        return component;
    }
}
