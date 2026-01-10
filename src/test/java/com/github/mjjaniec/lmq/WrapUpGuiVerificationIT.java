package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.github.mjjaniec.lmq.services.Results;
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

    record Expected(String name, List<Integer> rounds, int total, int playoff, int diff) {
    }

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
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
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
        gameService.setStage(wrapUp);


        // 4. Open Browsers and establish session first
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext()) {

            Page bigScreenPage = bigScreenContext.newPage();
            Page maestroPage = maestroContext.newPage();

            maestroPage.navigate(baseUrl + "/maestro/dj");
            maestroPage.getByTestId("maestro/dj/round-header-1").click();
            maestroPage.getByTestId("maestro/dj/wrapup/header").click();

            bigScreenPage.navigate(baseUrl + "/big-screen");
            assertThat(bigScreenPage.getByTestId("big-screen/top")).isVisible();


            var expected = List.of(
                    new Expected(p07, List.of(12, 32, 0, 8, 0, 20), 72, 264, 23),
                    new Expected(p04, List.of(0, 0, 15, 6, 32, 15), 68, 150, 91),
                    new Expected(p03, List.of(6, 36, 20, 4, 0, 0), 66, 204, 37),
                    new Expected(p09, List.of(10, 0, 6, 0, 36, 0), 52, 351, 110),
                    new Expected(p11, List.of(6, 0, 10, 10, 0, 15), 41, 112, 129),
                    new Expected(p06, List.of(8, 0, 15, 12, 0, 0), 35, 81, 160),
                    new Expected(p05, List.of(20, 0, 10, 0, 0, 4), 34, 400, 159),
                    new Expected(p08, List.of(0, 0, 0, 20, 0, 6), 26, 177, 64),
                    new Expected(p02, List.of(4, 0, 0, 10, 0, 10), 24, 71, 170),
                    new Expected(p12, List.of(0, 0, 5, 4, 0, 10), 19, 451, 210),
                    new Expected(p10, List.of(4, 0, 6, 4, 0, 5), 19, 471, 230),
                    new Expected(p01, List.of(10, 0, 0, 0, 0, 0), 10, 213, 28)
            );

            log.info("At the start table should have all rows hidden");
            for (int i = expected.size(); i >= 0; --i) {
                if (i < 3) {
                    checkPodium(bigScreenPage, i, p01, expected);
                } else {
                    checkTableVisibility(bigScreenPage, i, expected);
                }
                maestroPage.getByTestId("maestro/wrapup/show-more").click();
            }


            log.info("At the start table should have all rows hidden");
            for (int i = 0; i < expected.size(); i++) {
                var exp = expected.get(i);
                int pos = i + 1;
                log.info("Verifying position {}: {}", pos, exp.name);

                assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-" + pos)).hasText(exp.name);
                assertThat(bigScreenPage.getByTestId("big-screen/results/total-" + pos)).hasText(String.valueOf(exp.total));
                assertThat(bigScreenPage.getByTestId("big-screen/results/playoff-" + pos)).hasText(exp.playoff + " / " + playOff.value());

                for (int r = 0; r < exp.rounds.size(); r++) {
                    assertThat(bigScreenPage.getByTestId("big-screen/results/round-" + (r + 1) + "-" + pos))
                            .hasText(String.valueOf(exp.rounds.get(r)));
                }

                if (pos == 1) {
                    assertThat(bigScreenPage.getByTestId("big-screen/results/prize-" + pos)).hasText(Results.Award.FIRST.symbol);
                } else if (pos == 2) {
                    assertThat(bigScreenPage.getByTestId("big-screen/results/prize-" + pos)).hasText(Results.Award.SECOND.symbol);
                } else if (pos == 3) {
                    assertThat(bigScreenPage.getByTestId("big-screen/results/prize-" + pos)).hasText(Results.Award.THIRD.symbol);
                }
            }

            assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-12")).hasText(p01);
            assertThat(bigScreenPage.getByTestId("big-screen/results/prize-12")).hasText(Results.Award.PLAY_OFF.symbol);
        }
    }

    private void checkPodium(Page bigScreenPage, int visibleFrom, String playOffWinner, List<Expected> expects) {
        assertThat(bigScreenPage.getByTestId("big-screen/podium/playoffs")).containsText(playOffWinner);
        for (int i = 1; i <= 3; ++i) {
            var locator = bigScreenPage.getByTestId("big-screen/podium/segment-" + i);
            if (i <= visibleFrom) {
                assertThat(locator).isEmpty();
            } else {
                assertThat(locator).containsText(expects.get(i - 1).name);
            }
        }
    }

    private void checkTableVisibility(Page bigScreenPage, int visibleFrom, List<Expected> expects) {
        for (int pos = 1; pos <= expects.size(); ++pos) {
            var locator = bigScreenPage.getByTestId("big-screen/results/nickname-" + pos);
            assertThat(locator).hasText(expects.get(pos - 1).name);
            if (pos <= visibleFrom) {
                assertThat(locator).isHidden();
            } else {
                assertThat(locator).isVisible();
            }
        }
    }
}
