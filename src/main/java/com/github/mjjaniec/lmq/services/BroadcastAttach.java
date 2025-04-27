package com.github.mjjaniec.lmq.services;

import com.vaadin.flow.component.UI;


public interface BroadcastAttach {
    void attachPlayerUI(UI ui);

    void detachPlayerUI(UI ui);

    void attachBigScreenUI(UI ui);

    void detachBigScreenUI(UI ui);

    void attachPlayerList(UI ui, Runnable refresh);

    void detachPlayerList(UI ui);

    void attachSlackersList(UI ui, Runnable refresh);

    void detachSlackersList(UI ui);

    void attachProgressBar(UI ui, Runnable refresh);

    void detachProgressBar(UI ui);

    void attachPlay(UI ui, Runnable refresh);

    void detachPlay(UI ui);

    void attachWrapUp(UI ui, Runnable refresh);

    void detachWrapUp(UI ui);

    void attachPlayOff(UI ui, Runnable refresh);

    void detachPlayOff(UI ui);

    void attachBonusListener(UI ui, Runnable refresh);

    void detachBonusListener(UI ui);
}
