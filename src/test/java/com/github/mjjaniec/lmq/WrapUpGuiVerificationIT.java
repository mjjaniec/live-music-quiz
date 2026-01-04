package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.github.mjjaniec.lmq.stores.*;
import com.microsoft.playwright.*;
import com.vaadin.copilot.shaded.guava.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Slf4j
public class WrapUpGuiVerificationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private MaestroInterface gameService;

    @Autowired
    private PlayerStore playerStore;

    @Autowired
    private AnswerStore answerStore;

    @Autowired
    private QuizStore quizStore;

    @Autowired
    private PlayOffStore playOffStore;

    @Autowired
    private PlayOffTaskStore playOffTaskStore;

    @MockitoBean
    private SpreadsheetLoader spreadsheetLoader;

    private final PlayOffs.PlayOff playOff = new PlayOffs.PlayOff("the name", 2, 241);

    // We don't mock Navigator here because we want the real one to handle Vaadin's navigation
    // which is what we are testing with Playwright.

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        public SpreadsheetLoader spreadsheetLoader() {
            SpreadsheetLoader mock = Mockito.mock(SpreadsheetLoader.class);
            Mockito.when(mock.loadPlayOffs()).thenReturn(new PlayOffs(List.of()));
            return mock;
        }
    }

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void setup() {
        Mockito.when(spreadsheetLoader.loadPlayOffs()).thenReturn(new PlayOffs(List.of(playOff)));
        playerStore.clearPlayers();
        answerStore.clearAnswers();
        quizStore.clearQuiz();
    }

    @Test
    void injectDataAndVerifyGui() {
        String baseUrl = "http://localhost:" + port;

        // 1. Setup a minimal game structure
        MainSet.Piece piece1 = new MainSet.Piece("Artist1", null, "Title1", null, null, null, Set.of("ALL"));
        MainSet.LevelPieces level1 = new MainSet.LevelPieces(MainSet.RoundMode.EVERYBODY, List.of(piece1));
        MainSet.LevelPieces level2 = new MainSet.LevelPieces(MainSet.RoundMode.FIRST, List.of(piece1));
        MainSet.LevelPieces level3 = new MainSet.LevelPieces(MainSet.RoundMode.ONION, List.of(piece1));
        MainSet.LevelPieces level4 = new MainSet.LevelPieces(MainSet.RoundMode.EVERYBODY, List.of(piece1));
        MainSet.LevelPieces level5 = new MainSet.LevelPieces(MainSet.RoundMode.FIRST, List.of(piece1));
        MainSet.LevelPieces level6 = new MainSet.LevelPieces(MainSet.RoundMode.ONION, List.of(piece1));
        MainSet mainSet = new MainSet(List.of(level1, level2, level3, level4, level5, level6));

        gameService.reset();
        gameService.initGame(mainSet);

        playOffTaskStore.savePlayOffTask(playOff);

        String p01 = "Agnieszka";
        String p02 = "Barnuś :\uD83D\uDC15";
        String p03 = "Chromek";
        String p04 = "Daria da da da";
        String p05 = "Eryk I król Norwegii";
        String p06 = "Felicjana \uD83C\uDF55";
        String p07 = "Gosia";
        String p08 = "Hania";
        String p09 = "Jaro co się nie staro";
        String p10 = "Kududu dudutudu";
        String p11 = "Kaczabonga";
        String p12 = "Martynka";


        var players = List.of(p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12);

        var firstRoundPoints = List.of(10, 4, 6, 0, 20, 8, 12, 0, 10, 4, 6, 0);
        var secondRoundPoints = List.of(0, 0, 36, 0, 0, 0, 32, 0, 0, 0, 0, 0);
        var thirdRoundPoints = List.of(0, 0, 20, 15, 10, 15, 0, 0, 6, 6, 10, 5);
        var fourthRoundPoints = List.of(0, 10, 4, 6, 0, 12, 8, 20, 0, 4, 10, 4);
        var fifthRoundPoints = List.of(0, 0, 0, 32, 0, 0, 0, 0, 36, 0, 0, 0);
        var sixthRoundPoints = List.of(0, 10, 0, 15, 4, 0, 20, 6, 0, 5, 15, 10);

        var playOffs = List.of(213, 71, 204, 150, 400, 81, 264, 177, 351, 471, 112, 451);

        players.forEach(playerStore::addPlayer);

        var rounds = List.of(firstRoundPoints, secondRoundPoints, thirdRoundPoints, fourthRoundPoints, fifthRoundPoints, sixthRoundPoints);

        Streams.mapWithIndex(rounds.stream(), Map::entry)
                .forEach(r -> Streams.zip(players.stream(), r.getKey().stream(), Map::entry)
                        .forEach(p ->
                                answerStore.saveAnswer(new Answer(true, true, p.getValue(), p.getKey(), (int) (1 + r.getValue()), 1, null, null))));

        Streams.zip(players.stream(), playOffs.stream(), Map::entry)
                .forEach(entry -> playOffStore.savePlayOff(new Player(entry.getKey()), entry.getValue()));


        StageSet stageSet = Objects.requireNonNull(gameService.stageSet());
        GameStage.WrapUp wrapUp = stageSet.wrapUpStage();
        wrapUp.setDisplay(GameStage.Display.FULL_TABLE);
        gameService.setStage(wrapUp);


        // 4. Open Browsers and establish session first
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext()) {

            Page bigScreenPage = bigScreenContext.newPage();
            Page maestroPage = maestroContext.newPage();

            maestroPage.navigate(baseUrl + "/maestro/dj");
            maestroPage.getByTestId("maestro/dj/round-header-1").click();

            bigScreenPage.navigate(baseUrl + "/big-screen");
            assertThat(bigScreenPage.getByTestId("big-screen/top")).isVisible();


            // TODO verify table data here
        }
    }
}
