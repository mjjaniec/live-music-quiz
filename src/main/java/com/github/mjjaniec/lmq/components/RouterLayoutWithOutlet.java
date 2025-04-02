package com.github.mjjaniec.lmq.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.router.RouterLayout;

import java.util.Objects;

public interface RouterLayoutWithOutlet<C extends Component> extends RouterLayout {
    C outlet();

    default void showRouterLayoutContent(HasElement content) {
        if (content != null) {
            outlet().getElement().appendChild(Objects.requireNonNull(content.getElement()));
        }
    }
}
