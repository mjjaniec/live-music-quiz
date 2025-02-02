package com.github.mjjaniec.model;

import java.util.List;

public record QuizRound(RoundPoints points, List<QuizPiece> pieces, RoundMode mode) {
}
