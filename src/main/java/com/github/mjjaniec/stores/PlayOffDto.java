package com.github.mjjaniec.stores;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "play_off")
public class PlayOffDto {
    @Id
    private String player;
    private int value;
}