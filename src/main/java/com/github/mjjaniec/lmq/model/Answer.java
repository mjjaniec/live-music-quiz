package com.github.mjjaniec.lmq.model;

import javax.annotation.Nullable;

public record Answer(boolean artist, boolean title, int bonus, String player, int round, int piece, @Nullable String actualArtist, @Nullable String actualTitle) {
}
