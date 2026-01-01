package com.github.mjjaniec.lmq.components;

import com.github.mjjaniec.lmq.model.GameStage;
import com.vaadin.flow.component.button.Button;

import java.util.function.Consumer;


public class PieceStageButton extends Button {
    public PieceStageButton(GameStage.RoundPiece piece, GameStage.PieceStage pieceStage, Consumer<GameStage.RoundPiece> onActivate) {
        setText(caption(pieceStage));
        addClickListener(_ -> {
            piece.setCurrentStage(pieceStage);
            onActivate.accept(piece);
        });
    }

    private String caption(GameStage.PieceStage pieceStage) {
        return switch (pieceStage) {
            case LISTEN, ONION_LISTEN -> "\uD83C\uDFB5 Niech Słuchają";
            case REVEAL -> "\uD83D\uDCDC Odsłoń";
            case PLAY -> "▶ Gramy!";
        };
    }
}
