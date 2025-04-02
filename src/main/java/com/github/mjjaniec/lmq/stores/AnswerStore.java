package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Answer;

import java.util.Optional;
import java.util.stream.Stream;

public interface AnswerStore {
    void saveAnswer(Answer answer);

    void clearAnswers();

    Optional<Answer> playerAnswer(String player, int round, int piece);

    Stream<Answer> playerAnswers(String player, int round);

    Stream<Answer> allAnswers();

}
