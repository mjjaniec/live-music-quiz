package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Answer;

import java.util.Map;
import java.util.Optional;

public interface AnswerStore {
    void saveAnswer(Answer answer);

    Map<Integer, Map<String, Integer>> levelToPlayerToPoints();

    void clearAnswers();

    Optional<Answer> playerAnswer(String player, int round, int piece);

}
