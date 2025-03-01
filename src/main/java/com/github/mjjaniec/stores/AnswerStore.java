package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Answer;

import java.util.Map;

public interface AnswerStore {
    void saveAnswer(Answer answer);

    Map<Integer, Map<String, Integer>> levelToPlayerToPoints();

}
