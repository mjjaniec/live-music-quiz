package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.MainSet;

import java.util.Optional;

public interface QuizStore {
    void setQuiz(MainSet set);

    Optional<MainSet> getQuiz();

    void clearQuiz();
}
