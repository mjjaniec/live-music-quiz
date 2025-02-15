package com.github.mjjaniec.components;

import com.github.mjjaniec.model.GameStage;
import com.vaadin.flow.component.button.Button;

import java.util.function.Consumer;


public class PieceStageButton extends Button {
    public PieceStageButton(GameStage.RoundPiece piece, GameStage.PieceStage pieceStage, Consumer<GameStage> onActivate) {
        setText(caption(pieceStage));
        addClickListener(event -> {
            piece.setCurrentStage(pieceStage);
            onActivate.accept(piece);
        });
    }

    private String caption(GameStage.PieceStage pieceStage) {
        return switch (pieceStage) {
            case LISTEN -> "▶ Play";
            case ANSWER -> "� Guess";
        };
    }
}
