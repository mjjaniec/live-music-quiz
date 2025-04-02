package com.github.mjjaniec.lmq.stores;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "play_off_task")
public class PlayOffTaskDto {
    @Id
    private Integer id;
}