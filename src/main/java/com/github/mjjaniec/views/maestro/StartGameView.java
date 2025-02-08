package com.github.mjjaniec.views.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.mjjaniec.model.Quiz;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.R;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = R.Maestro.Start.PATH, layout = MaestroView.class)
public class StartGameView extends VerticalLayout implements RouterLayout {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static final Path QUIZES_PATH = Path.of("quizes");

    StartGameView(GameService gameService) {
        Set<String> gameFiles = Optional.ofNullable(QUIZES_PATH.toFile().listFiles()).stream().flatMap(Stream::of)
                .filter(file -> !file.isDirectory() && file.getName().endsWith(".yml"))
                .map(File::getName)
                .collect(Collectors.toSet());

        ComboBox<String> games = new ComboBox<>("Choose Game");
        games.setItems(gameFiles);
        Button start = new Button("Start");
        start.setEnabled(false);
        games.addValueChangeListener(event -> start.setEnabled(true));

        start.addClickListener(event -> {
            Quiz quiz;
            try {
                quiz = mapper.reader().readValue(QUIZES_PATH.resolve(Path.of(games.getValue())).toFile(), Quiz.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            gameService.setQuiz(quiz);
            UI.getCurrent().navigate("admin/dj");
        });
        add(games, start);
    }
}
