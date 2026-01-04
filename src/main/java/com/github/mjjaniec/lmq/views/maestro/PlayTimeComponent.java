package com.github.mjjaniec.lmq.views.maestro;

import com.github.mjjaniec.lmq.components.Audio;
import com.github.mjjaniec.lmq.model.Constants;
import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.model.Player;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.github.mjjaniec.lmq.util.Palette;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static com.github.mjjaniec.lmq.util.TestId.testId;

public class PlayTimeComponent extends VerticalLayout {
    public PlayTimeComponent(GameStage.RoundPiece piece, MaestroInterface gameService, Audio notification, Runnable refreshPlay) {
        add(new Paragraph("play time!"));
        String responder = piece.getCurrentResponder();
        if (piece.isCompleted()) {
            Paragraph p = new Paragraph("zgadnięte!");
            p.getStyle().setColor(Palette.GREEN);
            add(p);
        } else if (responder == null) {
            add(new Paragraph("czekamy na zgłoszenia"));
        } else {
            notification.play();
            add(new Paragraph("Odpowiada: " + responder));
            Checkbox artist = new Checkbox("artysta");
            if (Constants.UNKNOWN.equals(piece.piece.artist())) {
                piece.setArtistAnswered(1);
            }
            testId(artist, "maestro/dj/play/artist-checkbox-" + piece.roundNumber + "-" + piece.pieceNumber.number());
            artist.setEnabled(piece.getArtistAnswered() == 0);
            Checkbox title = new Checkbox("tytuł");
            testId(title, "maestro/dj/play/title-checkbox-" + piece.roundNumber + "-" + piece.pieceNumber.number());
            title.setEnabled(piece.getTitleAnswered() == 0);
            Button confirm = new Button("zatwierdź");
            testId(confirm, "maestro/dj/play/confirm-" + piece.roundNumber + "-" + piece.pieceNumber.number());
            confirm.addClickListener(_ -> {
                gameService.reportResult(
                        new Player(responder),
                        artist.getValue(),
                        title.getValue(),
                        null,
                        null);
                refreshPlay.run();
            });
            add(artist, title, confirm);
        }
    }
}
