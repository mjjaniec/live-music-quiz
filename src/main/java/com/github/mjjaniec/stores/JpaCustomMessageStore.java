package com.github.mjjaniec.stores;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Optional;

@Component
public interface JpaCustomMessageStore extends CrudRepository<CustomMessageDto, Long>, CustomMessageStore {

    @Override
    default Optional<String> readMessage() {
        Iterator<CustomMessageDto> result = findAll().iterator();
        if (result.hasNext()) {
            return Optional.of(result.next()).map(CustomMessageDto::getMessage);
        } else {
            return Optional.empty();
        }
    }

    @Override
    default void clearMessage() {
        deleteAll();
    }

    @Override
    default void setMessage(String message) {
        clearMessage();
        var cmd = new CustomMessageDto();
        cmd.setMessage(message);
        save(cmd);
    }
}
