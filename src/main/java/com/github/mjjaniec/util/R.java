package com.github.mjjaniec.util;

import lombok.experimental.Accessors;

import java.util.Optional;



public class R {
    public interface RI {
        Optional<RI> parent();

        String segment();

        default String get() {
            return parent().map(p -> p.get() + "/").orElse("") + segment();
        }
    }

    public record Player(String segment, Optional<RI> parent) implements RI {
        public static final String PATH = "player";
        public static final Maestro IT = new Maestro(PATH, Optional.empty());

        public record Join(String segment, Optional<RI> parent) implements RI {
            public static final String PATH = "join";
            public static final Join IT = new Join(PATH, Optional.of(Player.IT));
        }

        public record Answer(String segment, Optional<RI> parent) implements RI {
            public static final String PATH = "start";
            public static final Answer IT = new Answer(PATH, Optional.of(Player.IT));
        }
    }
    public record BigScreen(String segment, Optional<RI> parent) implements RI {
        public static final String PATH = "big-screen";
        public static final BigScreen IT = new BigScreen(PATH, Optional.empty());

        public record Invite(String segment, Optional<RI> parent) implements RI {
            public static final String PATH = "invite";
            public static final Invite IT = new Invite(PATH, Optional.of(BigScreen.IT));
        }
    }

    public record Maestro(String segment, Optional<RI> parent) implements RI {
        public static final String PATH = "maestro";
        public static final Maestro IT = new Maestro(PATH, Optional.empty());

        public record DJ(String segment, Optional<RI> parent) implements RI {
            public static final String PATH = "dj";
            public static final DJ IT = new DJ(PATH, Optional.of(Maestro.IT));
        }

        public record Start(String segment, Optional<RI> parent) implements RI {
            public static final String PATH = "start";
            public static final Start IT = new Start(PATH, Optional.of(Maestro.IT));
        }
    }
}


