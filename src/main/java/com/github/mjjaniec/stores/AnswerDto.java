package com.github.mjjaniec.stores;

import jakarta.persistence.*;
import lombok.Data;

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
}
