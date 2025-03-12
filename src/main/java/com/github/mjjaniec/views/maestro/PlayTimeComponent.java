package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.MaestroInterface;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class PlayTimeComponent extends VerticalLayout {
    public PlayTimeComponent(GameStage.RoundPiece piece, MaestroInterface gameService) {
        add(new Paragraph("play time!"));
        String responder = piece.getCurrentResponder();
        if (responder == null) {
            add(new Paragraph("czekamy na zgłoszenia"));
        } else {
            add(new Paragraph("Odpowiada: " + responder));
            Checkbox artist = new Checkbox("artysta");
            Checkbox title = new Checkbox("tytuł");
            Button confirm = new Button("zatwierdź");
            confirm.addClickListener(event -> {
                gameService.reportResult(new Player(responder), artist.getValue(), title.getValue(), piece.getFailedResponders().size() + 1);
                if (!artist.getValue() || !title.getValue()) {
                    piece.addFailedResponder(responder);
                } else {
                    piece.setCurrentResponder(null);
                }
                gameService.setStage(piece);
            });
            add(artist, title, confirm);
        }
    }
}
