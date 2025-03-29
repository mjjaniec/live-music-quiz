package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.PlayOffs;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Optional;

@Component
public interface JpaPlayOffTaskStore extends CrudRepository<PlayOffTaskDto, Integer>, PlayOffTaskStore {


    @Override
    default void clearPlayOffTask() {
        deleteAll();
    }

    @Override
    default Optional<PlayOffs.PlayOff> getPlayOffTask(PlayOffs playOffs) {
        Iterator<PlayOffTaskDto> result = findAll().iterator();
        if (result.hasNext()) {
            return Optional.of(result.next())
                    .flatMap(dto -> playOffs.playOffs().stream().filter(p -> p.id() == dto.getId())
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
