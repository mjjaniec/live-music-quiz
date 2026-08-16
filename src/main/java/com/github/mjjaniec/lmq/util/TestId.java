package com.github.mjjaniec.lmq.util;

import com.github.mjjaniec.lmq.components.DangerAction;
import com.vaadin.flow.component.Component;

public class TestId {

    public static <C extends Component> C testId(C component, String testId) {
        component.getElement().setAttribute("data-testid", testId);
        return component;
    }

    public static DangerAction testId(DangerAction component, String checkboxTestId, String buttonTestId) {
        testId(component.getDangerCheckbox(), checkboxTestId);
        testId(component.getActionButton(), buttonTestId);
        return component;
    }
}
