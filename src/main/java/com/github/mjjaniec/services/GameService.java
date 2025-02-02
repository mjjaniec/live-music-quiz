package com.github.mjjaniec.services;


import com.github.mjjaniec.model.GameLevel;
import com.github.mjjaniec.model.Quiz;
import com.vaadin.flow.component.UI;

public interface GameService {
    GameLevel currentLevel();

    void startListening();
    void endListening();
    void advance();

    void setQuiz(Quiz quiz);

    Quiz quiz();

    void addPlayerUi(UI ui);
    void removePlayerUi(UI ui);

}
