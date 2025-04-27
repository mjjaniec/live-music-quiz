package com.github.mjjaniec.lmq.stores;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@Entity
@Table(name = "answer")
public class AnswerDto {

    @Id
    private String id;

    private boolean artist;
    private boolean title;
    private int bonus;
    private String player;
    private int round;
    private int piece;
    @Nullable
    private String actualArtist;
    @Nullable
    private String actualTitle;
}
