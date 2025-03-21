package com.github.mjjaniec.components;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.html.Paragraph;

import java.util.List;
import java.util.stream.Collectors;

public class LittleSlackerList extends Paragraph {
    public LittleSlackerList(List<Player> slackers) {
        if (slackers.isEmpty()) {
            setText("Wszyscy odpowiedzieli!");
            getStyle().setColor(Palette.GREEN);
        } else {
            setText("Czekamy na: " + slackers.stream().map(Player::name).collect(Collectors.joining(", ")));
        }
    }
}
