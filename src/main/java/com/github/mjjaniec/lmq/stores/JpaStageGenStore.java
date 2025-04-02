package com.github.mjjaniec.lmq.stores;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public interface JpaStageGenStore extends CrudRepository<StageDto, Long> {
}
