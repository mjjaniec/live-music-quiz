package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Answer;

import java.util.Optional;
import java.util.stream.Stream;

public interface AnswerStore {
    void saveAnswer(Answer answer);

    void clearAnswers();

    Optional<Answer> playerAnswer(String player, int round, int piece);

    Stream<Answer> playerAnswers(String player, int round);

    Stream<Answer> allAnswers();

}
