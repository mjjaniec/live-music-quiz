package com.github.mjjaniec.lmq.stores;

import org.apache.commons.lang3.stream.Streams;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface JpaFeedbackStore extends CrudRepository<FeedbackDto, Long>, FeedbackStore {
    @Override
    default void saveFeedback(String message) {
        var dto = new FeedbackDto();
        dto.setMessage(message);
        save(dto);
    }

    @Override
    default List<String> readFeedback() {
        return Streams.of(findAll()).map(FeedbackDto::getMessage).toList();
    }
}
