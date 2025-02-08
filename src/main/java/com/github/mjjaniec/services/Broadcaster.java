package com.github.mjjaniec.services;

import com.vaadin.flow.component.UI;


public interface Broadcaster {
    void attachPlayerUI(UI ui);
    void detachPlayerUI(UI ui);

    void attachBigScreenUI(UI ui);
    void detachBigScreenUI(UI ui);
}
