package com.github.mjjaniec.lmq.stores;

import jakarta.persistence.*;
import java.util.List;
import lombok.Data;
import org.jspecify.annotations.Nullable;

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

        private String roundMode;

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

        @Nullable
        private String artistAlternative;

        private String title;

        @Nullable
        private String titleAlternative;

        @Nullable
        private Integer tempo;

        @Nullable
        private String hint;
    }
}
