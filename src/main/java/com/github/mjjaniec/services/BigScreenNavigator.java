package com.github.mjjaniec.services;

import com.github.mjjaniec.views.bigscreen.BigScreenRoute;
import com.vaadin.flow.component.Component;

public interface BigScreenNavigator {
    <T extends Component & BigScreenRoute> void navigateBigScreen(Class<T> view);
    void refreshPlayerLists();
}
