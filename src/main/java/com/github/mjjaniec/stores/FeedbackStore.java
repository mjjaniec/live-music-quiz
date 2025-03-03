package com.github.mjjaniec.stores;

import java.util.List;

public interface FeedbackStore {
    void saveFeedback(String message);
    List<String> readFeedback();
}
