package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.PlayOffs;
import org.springframework.data.repository.CrudRepository;

import java.util.Iterator;
import java.util.Optional;

public interface JpaPlayOffTaskStore extends CrudRepository<PlayOffTaskDto, Integer>, PlayOffTaskStore {
    @Override
    default void clearPlayOffTask() {
        deleteAll();
    }

    @Override
    default Optional<PlayOffs.PlayOff> getPlayOffTask() {
        Iterator<PlayOffTaskDto> result = findAll().iterator();
        if (result.hasNext()) {
            return Optional.of(result.next())
                    .flatMap(dto -> PlayOffs.ThePlayOffs.playOffs().stream().filter(p -> p.id() == dto.getId())
                            .findFirst());
        } else {
            return Optional.empty();
        }
    }

    @Override
    default void savePlayOffTask(PlayOffs.PlayOff task) {
        PlayOffTaskDto dto = new PlayOffTaskDto();
        dto.setId(task.id());
        save(dto);
    }
}
