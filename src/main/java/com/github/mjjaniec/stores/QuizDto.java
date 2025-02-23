package com.github.mjjaniec.stores;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.util.List;

@Entity
@Data
@Table(name = "quiz")
public class QuizDto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Level> levels;

    @Entity
    @Data
    @Table(name = "level")
    static class Level {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;
        private String difficulty;
        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
        private List<Piece> pieces;
    }

    @Entity
    @Data
    @Table(name = "piece")
    static class Piece {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;
        private String artist;
        private String title;
        private String instrument;
        @Nullable
        private Integer tempo;
        @Nullable
        private String hint;
    }

}
