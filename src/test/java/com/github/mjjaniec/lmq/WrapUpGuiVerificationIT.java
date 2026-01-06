package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.github.mjjaniec.lmq.services.Results;
import com.github.mjjaniec.lmq.stores.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
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
        wrapUp.setDisplay(GameStage.Display.FULL_TABLE);
        gameService.setStage(wrapUp);


        // 4. Open Browsers and establish session first
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext()) {

            Page bigScreenPage = bigScreenContext.newPage();
            Page maestroPage = maestroContext.newPage();

            maestroPage.navigate(baseUrl + "/maestro/dj");
            maestroPage.getByTestId("maestro/dj/round-header-1").click();
            maestroPage.getByTestId("maestro/dj/wrapup-header").click();

            bigScreenPage.navigate(baseUrl + "/big-screen");
            assertThat(bigScreenPage.getByTestId("big-screen/top")).isVisible();


            record Expected(String name, List<Integer> rounds, int total, int playoff, int diff) {}
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

            // Verify the PLAY_OFF award for the person with the best playoff result outside of podium
            // In our case:
            // 1. Felicjana: 72, diff 23 (FIRST)
            // 2. Daria: 68, diff 91 (SECOND)
            // 3. Chromek: 66, diff 37 (THIRD)
            // 4. Jaro: 52, diff 110
            // 5. Kududu: 41, diff 129
            // 6. Eryk: 35, diff 160
            // 7. Hania: 34, diff 159
            // 8. Gosia: 26, diff 64
            // 9. Barnuś: 24, diff 170
            // 10. Martynka: 19, diff 210
            // 11. Jaro co się: 19, diff 230
            // 12. Agnieszka: 10, diff 28  <-- BEST DIFF outside podium

            assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-12")).hasText(p01);
            assertThat(bigScreenPage.getByTestId("big-screen/results/prize-12")).hasText(Results.Award.PLAY_OFF.symbol);


            log.info("Verifying visibility control - SIXTH option");
            // Switch to SIXTH option via Maestro
            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("SIXTH")).click();

            // Rows with position < 6 should be hidden.
            // Actually, ResultsTable.java: Stream.of("hidden").filter(ignored -> row.position() < showFrom)
            // If showFrom is 6, then position 1, 2, 3, 4, 5 should be hidden.
            for (int pos = 6; pos <= 12; pos++) {
                assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-" + pos)).isVisible();
            }
            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("SIXTH")).click();
            for (int pos = 1; pos <= 5; pos++) {
                assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-" + pos)).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(15000));
            }

            log.info("Verifying visibility control - FOURTH option");
            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("FOURTH")).click();
            for (int pos = 4; pos <= 12; pos++) {
                assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-" + pos)).isVisible();
            }
            for (int pos = 1; pos <= 3; pos++) {
                assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-" + pos)).isHidden();
            }

            log.info("Verifying visibility control - FULL_TABLE option");
            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("FULL_TABLE")).click();
            for (int pos = 1; pos <= 12; pos++) {
                assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-" + pos)).isVisible();
            }

            log.info("Verifying podium");
            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("EMPTY_PODIUM")).click();
            bigScreenPage.getByTestId("big-screen/podium").isVisible();

            assertThat(bigScreenPage.getByTestId("big-screen/podium/playoffs")).containsText(p01);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-3")).isEmpty();
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-2")).isEmpty();
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-1")).isEmpty();

            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("THIRD_PODIUM")).click();
            assertThat(bigScreenPage.getByTestId("big-screen/podium/playoffs")).containsText(p01);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-3")).containsText(p03);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-2")).isEmpty();
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-1")).isEmpty();

            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("SECOND_PODIUM")).click();
            assertThat(bigScreenPage.getByTestId("big-screen/podium/playoffs")).containsText(p01);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-3")).containsText(p03);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-2")).containsText(p04);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-1")).isEmpty();

            maestroPage.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("FULL_PODIUM")).click();
            assertThat(bigScreenPage.getByTestId("big-screen/podium/playoffs")).containsText(p01);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-3")).containsText(p03);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-2")).containsText(p04);
            assertThat(bigScreenPage.getByTestId("big-screen/podium/segment-1")).containsText(p07);
        }
    }
}
