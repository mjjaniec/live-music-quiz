package com.github.mjjaniec.stores;

import java.util.Optional;

public interface CustomMessageStore {

    Optional<String> readMessage();
    void clearMessage();
    void setMessage(String message);
}
