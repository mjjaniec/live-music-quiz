package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameLevel;
import com.github.mjjaniec.model.Quiz;
import com.github.mjjaniec.model.RoundMode;
import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GameServiceImpl implements GameService {
    private Quiz quiz;
    private final List<UI> playerUis = new ArrayList<>();
    private final List<UI> publicUis = new ArrayList<>();


    @Override
    public GameLevel currentLevel() {
        return new GameLevel(1, 1, 4, 2, RoundMode.EVERYBODY);
    }

    @Override
    public void startListening() {

    }

    @Override
    public void endListening() {

    }

    @Override
    public void advance() {

    }

    @Override
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        playerUis.forEach(ui -> ui.access(() -> ui.navigate("player/join")));
    }

    @Override
    public Quiz quiz() {
        return quiz;
    }


    @Override
    public void addPlayerUi(UI ui) {
        playerUis.add(ui);
    }

    @Override
    public void removePlayerUi(UI ui) {
        playerUis.remove(ui);
    }
}
