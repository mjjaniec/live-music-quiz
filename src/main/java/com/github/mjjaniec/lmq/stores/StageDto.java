package com.github.mjjaniec.lmq.stores;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stage")
public class StageDto {
    static final int summary = 999;
    static final int playOff = 998;
    static final int init = -1;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private int round;
    private int piece;
    @Column(length = 4096)
    private String additions;

    void set(int round, int piece) {
        this.round = round;
        this.piece = piece;
    }

    void set(int round, int piece, String additions) {
        set(round, piece);
        this.additions = additions;
    }
}
