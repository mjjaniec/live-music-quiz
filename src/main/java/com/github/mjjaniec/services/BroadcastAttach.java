package com.github.mjjaniec.services;

import com.vaadin.flow.component.UI;


public interface BroadcastAttach {
    void attachPlayerUI(UI ui);
    void detachPlayerUI(UI ui);

    void attachBigScreenUI(UI ui);
    void detachBigScreenUI(UI ui);

    void attachPlayerList(UI  ui, Runnable refresh);
    void detachPlayerList(UI  ui);

    void attachSlackersList(UI ui, Runnable refresh);
    void detachSlackersList(UI ui);
}
