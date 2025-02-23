package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.MainSet;

import java.util.Optional;

public interface QuizStore {
    void setQuiz(MainSet set);
    Optional<MainSet> getQuiz();
    void clearQuiz();
}
