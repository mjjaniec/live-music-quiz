package com.github.mjjaniec.lmq.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Protects a dangerous/irreversible action behind a disabled-by-default button paired with a "danger"
 * checkbox that enables it, instead of a confirmation popup. Callers apply their own testids to
 * {@link #getActionButton()}/{@link #getDangerCheckbox()} and may further customize the button (e.g. theme
 * variants) after construction.
 */
public class DangerAction extends HorizontalLayout {

    private final Checkbox dangerCheckbox = new Checkbox("danger", false);
    private final Button actionButton;

    public DangerAction(String buttonLabel, ComponentEventListener<ClickEvent<Button>> onClick) {
        actionButton = new Button(buttonLabel, onClick);
        actionButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        actionButton.setEnabled(false);
        dangerCheckbox.addValueChangeListener(event -> actionButton.setEnabled(event.getValue()));
        add(dangerCheckbox, actionButton);
    }

    public void click() {
        actionButton.click();
    }

    public Button getActionButton() {
        return actionButton;
    }

    public Checkbox getDangerCheckbox() {
        return dangerCheckbox;
    }
}
