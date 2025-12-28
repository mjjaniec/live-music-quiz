package com.github.mjjaniec.lmq.model;

import org.jspecify.annotations.Nullable;

public record Answer(boolean artist, boolean title, int points, String player, int round, int piece, @Nullable String actualArtist, @Nullable String actualTitle) {
}
