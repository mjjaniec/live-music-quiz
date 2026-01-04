package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.model.Constants;
import com.github.mjjaniec.lmq.util.Palette;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Slf4j
public class GameFlowIT {
    private static final int PORT = Integer.parseInt(System.getProperty("server.port", "8090"));
    private static final String BASE_URL = "http://localhost:" + PORT;

    private record PieceInfo(String artist,String title) {
    }

    static Playwright playwright;
    static Browser browser;

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

    @Test
    void startingTheGameFlow() {
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext();
             BrowserContext player1Context = browser.newContext();
             BrowserContext player2Context = browser.newContext()) {

            Page maestroPage = maestroContext.newPage();
            Page bigScreenPage = bigScreenContext.newPage();
            Page player1Page = player1Context.newPage();
            Page player2Page = player2Context.newPage();

            // 1. Maestro starts the game
            initTheGame(maestroPage, bigScreenPage);

            //  Players join
            joinPlayer(player1Page, "Player1");
            joinPlayer(player2Page, "Player2");

            //  Verify players on big screen
            assertThat(bigScreenPage.getByTestId("big-screen/players-container")).containsText("Player1");
            assertThat(bigScreenPage.getByTestId("big-screen/players-container")).containsText("Player2");
            assertThat(bigScreenPage.getByText("grają z nami! (2 os)")).isVisible();

            //  Verify players on maestro
            assertThat(maestroPage.getByTestId("maestro/players-grid")).containsText("Player1");
            assertThat(maestroPage.getByTestId("maestro/players-grid")).containsText("Player2");
        }
    }

    @Test
    void uniquePlayerNameFlow() {
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext();
             BrowserContext player1Context = browser.newContext();
             BrowserContext player2Context = browser.newContext()) {

            Page maestroPage = maestroContext.newPage();
            Page bigScreenPage = bigScreenContext.newPage();
            Page player1Page = player1Context.newPage();
            Page player2Page = player2Context.newPage();

            //  Maestro starts the game
            initTheGame(maestroPage, bigScreenPage);

            // Player 1 joins successfully
            log.info("unique: Player 1 joining");
            joinPlayer(player1Page, "UniquePlayer");

            // Player 2 tries to join with same nickname
            log.info("unique: Player 2 joining with same nickname");
            player2Page.navigate(BASE_URL + "/");
            player2Page.locator("vaadin-text-field[data-testid='player/join/nickname'] input").fill("UniquePlayer");
            player2Page.getByTestId("player/join/button").click();

            // Verify error message
            assertThat(player2Page.getByText("kswyka zajęta, wybierz inną")).isVisible();
            log.info("unique: Error message visible");

            // Player 2 changes nickname and joins successfully
            log.info("unique: Player 2 joining with different nickname");
            player2Page.locator("vaadin-text-field[data-testid='player/join/nickname'] input").fill("UniquePlayer2");
            player2Page.getByTestId("player/join/button").click();
            assertThat(player2Page.getByText("poczekaj na pozostałych graczy")).isVisible();

            // Verify both on big screen
            log.info("unique: Verifying on Big Screen");
            assertThat(bigScreenPage.getByTestId("big-screen/players-container")).containsText("UniquePlayer");
            assertThat(bigScreenPage.getByTestId("big-screen/players-container")).containsText("UniquePlayer2");

            // Verify both on maestro
            log.info("unique: Verifying on Maestro");
            assertThat(maestroPage.getByTestId("maestro/players-grid")).containsText("UniquePlayer");
            assertThat(maestroPage.getByTestId("maestro/players-grid")).containsText("UniquePlayer2");
        }
    }

    @Test
    void bigScreenMessageFlow() {
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext()) {

            Page maestroPage = maestroContext.newPage();
            Page bigScreenPage = bigScreenContext.newPage();

            // 1. Maestro starts the game
            initTheGame(maestroPage, bigScreenPage);

            // 2. Maestro sets a public message
            String testMessage = "Hello Integration Test!";
            log.info("Setting public message: {}", testMessage);
            maestroPage.locator("vaadin-text-field[data-testid='maestro/dj/message-field'] input").fill(testMessage);
            // Wait for button to change text to "Ustaw"
            assertThat(maestroPage.getByTestId("maestro/dj/message-button")).hasText("Ustaw");
            maestroPage.getByTestId("maestro/dj/message-button").click();

            // 3. Verify message on big screen
            log.info("Verifying message on Big Screen");
            assertThat(bigScreenPage.getByTestId("big-screen/custom-message")).hasText(testMessage);

            // 4. Clear the message
            log.info("Cleaning public message");
            maestroPage.locator("vaadin-text-field[data-testid='maestro/dj/message-field'] input").fill("");
            // Wait for button to change text to "Wyczyść"
            assertThat(maestroPage.getByTestId("maestro/dj/message-button")).hasText("Wyczyść");
            maestroPage.getByTestId("maestro/dj/message-button").click();

            // 5. Verify logo (or at least that message is gone)
            log.info("Verifying message is gone from Big Screen");
            assertThat(bigScreenPage.getByTestId("big-screen/custom-message-container")).isHidden();
        }
    }

    @Test
    void bumpOutPlayerFlow() {
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext();
             BrowserContext playerContext = browser.newContext()) {

            Page maestroPage = maestroContext.newPage();
            Page bigScreenPage = bigScreenContext.newPage();
            Page playerPage = playerContext.newPage();

            // 1. Maestro starts the game
            initTheGame(maestroPage, bigScreenPage);

            // 2. One player joins
            log.info("bump: Player joining");
            joinPlayer(playerPage, "ToBoBumped");

            // 3. Maestro bumps him out
            log.info("bump: Maestro bumping out player");

            maestroPage.getByTestId("mastero/players-grid/danger/ToBoBumped").click();
            maestroPage.getByTestId("mastero/players-grid/bump-out/ToBoBumped").click();

            // 4. Player is redirected to join page
            log.info("bump: Verifying player redirected to join page");
            // Vaadin might take a moment to redirect
            playerPage.waitForURL(url -> url.contains("/player/join"));
            assertThat(playerPage.getByTestId("player/join/button")).isVisible();

            // 5. Player rejoins under new nickname
            log.info("bump: Player rejoining with new nickname");
            playerPage.locator("vaadin-text-field[data-testid='player/join/nickname'] input").fill("RejoinedPlayer");
            playerPage.getByTestId("player/join/button").click();
            assertThat(playerPage.getByText("poczekaj na pozostałych graczy")).isVisible();

            // 6. Verify visible on big screen
            log.info("bump: Verifying on Big Screen");
            assertThat(bigScreenPage.getByTestId("big-screen/players-container")).containsText("RejoinedPlayer");
            assertThat(bigScreenPage.getByTestId("big-screen/players-container")).not().containsText("ToBoBumped");
        }
    }

    @Test
    void gameInEverybodyModeFlow() {
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext();
             BrowserContext p1Context = browser.newContext();
             BrowserContext p2Context = browser.newContext();
             BrowserContext p3Context = browser.newContext()) {

            Page maestroPage = maestroContext.newPage();
            Page bigScreenPage = bigScreenContext.newPage();
            Page p1Page = p1Context.newPage();
            Page p2Page = p2Context.newPage();
            Page p3Page = p3Context.newPage();
            List.of(p1Page, p2Page, p3Page).forEach(p -> p.setViewportSize(480, 640));

            // 1. Maestro starts the game
            initTheGame(maestroPage, bigScreenPage);

            // 2. 3 players join
            log.info("everybody: Players joining");
            joinPlayer(p1Page, "P1");
            joinPlayer(p2Page, "P2");
            joinPlayer(p3Page, "P3");

            // 3. Maestro starts the first round
            log.info("everybody: Starting first round");
            maestroPage.getByTestId("maestro/dj/round-header-1").click();
            maestroPage.getByTestId("maestro/dj/round-init/activate-1").click();

            // 4. On big screen info about that round is displayed
            log.info("everybody: Verifying round info on Big Screen");
            assertThat(bigScreenPage.getByTestId("big-screen/progress-bar/Runda")).containsText("Runda:  1 /");

            // 5. Players see information that round is about to start
            log.info("everybody: Verifying players see wait-for-round");
            assertThat(p1Page.getByTestId("player/wait-for-round")).isVisible();
            assertThat(p2Page.getByTestId("player/wait-for-round")).isVisible();
            assertThat(p3Page.getByTestId("player/wait-for-round")).isVisible();

            // 6. Then maestro selects first piece
            log.info("everybody: Selecting first piece");
            var info = expandPiece(maestroPage, 1, 1);
            log.info(info.toString());
            maestroPage.getByTestId("maestro/dj/piece-LISTEN-1-1").click();
            validateSlackers(bigScreenPage, List.of("P1", "P2", "P3"));

            // 7. Users provide answers
            log.info("everybody: Players providing answers");
            provideAnswer(p1Page, info, info.artist, info.title);
            validateSlackers(bigScreenPage, List.of("P2", "P3"));
            provideAnswer(p2Page, info, info.artist, null);
            validateSlackers(bigScreenPage, List.of("P3"));
            provideAnswer(p3Page, info, "Metallica", "Nothing Else Matters");
            validateSlackers(bigScreenPage, List.of());

            // 8. Maestro reveals correct answers and players see their points
            log.info("everybody: Maestro revealing answers");
            maestroPage.getByTestId("maestro/dj/piece-REVEAL-1-1").click();

            log.info("everybody: Verifying points");
            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("10");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("4");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("0");

            log.info("everybody: Verifying big screen reveal");
            assertThat(bigScreenPage.getByText(info.artist)).isVisible();
            assertThat(bigScreenPage.getByText(info.title)).isVisible();

            // 9. Second piece of round 1
            log.info("everybody: Selecting second piece");
            var info2 = expandPiece(maestroPage, 1, 2);
            maestroPage.getByTestId("maestro/dj/piece-LISTEN-1-2").click();

            log.info("maestro: Enabling bonus mode");
            maestroPage.getByTestId("maestro/dj/piece-bonus").click();
            assertThat(bigScreenPage.getByText("Słuchaj i zgarnij BONUS!")).isVisible();

            log.info("everybody: Players providing answers (P1 dunno, P2 correct)");
            provideAnswer(p1Page, info2, null, null);
            validateSlackers(bigScreenPage, List.of("P2", "P3"));
            provideAnswer(p2Page, info2, info2.artist, info2.title);
            validateSlackers(bigScreenPage, List.of("P3"));

            log.info("maestro: Disabling bonus mode");
            maestroPage.getByTestId("maestro/dj/piece-bonus").click();
            assertThat(bigScreenPage.getByText("Słuchaj, słuchaj jaj jaj")).isVisible();

            log.info("everybody: P3 providing answers (artist wrong, title correct)");
            provideAnswer(p3Page, info2, "Metallica", info2.title);
            validateSlackers(bigScreenPage, List.of());

            log.info("everybody: Maestro revealing answers for 1-2");
            maestroPage.getByTestId("maestro/dj/piece-REVEAL-1-2").click();

            log.info("everybody: Verifying points for 1-2");
            // P1: 0 (dunno)
            // P2: 20 (artist 4*2 + title 6*2) - because bonus was enabled when P2 answered
            // P3: 6 (artist incorrect, title 6*1) - because bonus was disabled when P3 answered
            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("0");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("20");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("6");

            log.info("everybody: Verifying big screen reveal for 1-2");
            assertThat(bigScreenPage.getByText(info2.artist)).isVisible();
            assertThat(bigScreenPage.getByText(info2.title)).isVisible();

            // 10. Third piece of round 1 (unknown artist)
            log.info("everybody: Selecting third piece");
            var info3 = expandPiece(maestroPage, 1, 3);
            maestroPage.getByTestId("maestro/dj/piece-LISTEN-1-3").click();

            log.info("everybody: Players providing answers (P1 dunno, P2 correct, P3 wrong)");
            provideAnswer(p1Page, info3, null, null);
            validateSlackers(bigScreenPage, List.of("P2", "P3"));
            provideAnswer(p2Page, info3, null, info3.title);
            validateSlackers(bigScreenPage, List.of("P3"));
            provideAnswer(p3Page, info3, null, info.title); // Use title from first piece as wrong answer
            validateSlackers(bigScreenPage, List.of());

            log.info("everybody: Maestro revealing answers for 1-3");
            maestroPage.getByTestId("maestro/dj/piece-REVEAL-1-3").click();

            log.info("everybody: Verifying points for 1-3");
            // P1: 0 (dunno)
            // P2: 6 (title correct)
            // P3: 0 (title wrong)
            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("0");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("6");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("0");

            log.info("everybody: Verifying big screen reveal for 1-3");
            assertThat(bigScreenPage.getByText(info3.title)).isVisible();

            log.info("everybody: Maestro activating round 1 summary");
            maestroPage.getByTestId("maestro/dj/round-summary-header-1").click();
            maestroPage.getByTestId("maestro/dj/round-summary-activate-1").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Aktywuj")).click();

            log.info("everybody: Verifying player round summary points");
            // P1: 10 + 0 + 0 = 10
            // P2: 4 + 20 + 6 = 30
            // P3: 0 + 6 + 0 = 6
            assertThat(p1Page.locator("h1")).hasText("10");
            assertThat(p2Page.locator("h1")).hasText("30");
            assertThat(p3Page.locator("h1")).hasText("6");

            // 11. Verify that big screen displays correct table with results. players should be ordered by their points in descending manner.
            log.info("everybody: Verifying results table on Big Screen");
            // P1: 10, P2: 30, P3: 6
            // Order should be: P2 (1), P1 (2), P3 (3)

            // P2 at position 1
            assertThat(bigScreenPage.getByTestId("big-screen/results/position-1")).hasText("1");
            assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-1")).hasText("P2");
            assertThat(bigScreenPage.getByTestId("big-screen/results/total-1")).hasText("30");

            // P1 at position 2
            assertThat(bigScreenPage.getByTestId("big-screen/results/position-2")).hasText("2");
            assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-2")).hasText("P1");
            assertThat(bigScreenPage.getByTestId("big-screen/results/total-2")).hasText("10");

            // P3 at position 3
            assertThat(bigScreenPage.getByTestId("big-screen/results/position-3")).hasText("3");
            assertThat(bigScreenPage.getByTestId("big-screen/results/nickname-3")).hasText("P3");
            assertThat(bigScreenPage.getByTestId("big-screen/results/total-3")).hasText("6");
        }
    }

    @Test
    void gameInFirstModeFlow() {
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext();
             BrowserContext p1Context = browser.newContext();
             BrowserContext p2Context = browser.newContext();
             BrowserContext p3Context = browser.newContext()) {

            Page maestroPage = maestroContext.newPage();
            Page bigScreenPage = bigScreenContext.newPage();
            Page p1Page = p1Context.newPage();
            Page p2Page = p2Context.newPage();
            Page p3Page = p3Context.newPage();
            List.of(p1Page, p2Page, p3Page).forEach(p -> p.setViewportSize(480, 640));

            // 1. Maestro starts the game
            initTheGame(maestroPage, bigScreenPage);

            // 2. 3 players join
            log.info("first: Players joining");
            joinPlayer(p1Page, "P1");
            joinPlayer(p2Page, "P2");
            joinPlayer(p3Page, "P3");

            // 3. Maestro starts the second round (FIRST mode)
            log.info("first: Starting second round");
            maestroPage.getByTestId("maestro/dj/round-header-2").click();
            maestroPage.getByTestId("maestro/dj/round-init/activate-2").click();

            // 4. On big screen info about that round is displayed
            log.info("first: Verifying round info on Big Screen");
            assertThat(bigScreenPage.getByTestId("big-screen/progress-bar/Runda")).containsText("Runda:  2 /");

            // 5. Maestro selects the first piece
            log.info("first: Selecting first piece");
            var info = expandPiece(maestroPage, 2, 1);
            maestroPage.getByTestId("maestro/dj/piece-PLAY-2-1").click();

            // 6. Proper info is displayed on big screen
            log.info("first: Verifying big screen has call to action");
            assertThat(bigScreenPage.getByText("No i zgłoś się!")).isVisible();

            // 7. A player volunteer to answer by clicking a button
            log.info("first: P1 volunteering to answer");
            assertThat(p1Page.getByTestId("player/play")).isVisible();
            Locator p1Button = p1Page.getByTestId("player/play/button");
            assertThat(p1Button).hasCSS("background-color", Palette.BLUE);
            p1Button.click();

            // 8. After that their name is displayed on the big screen and other players see their buttons disabled.
            log.info("first: Verifying P1 is the responder");
            assertThat(bigScreenPage.getByText("Odpowiada:")).isVisible();
            assertThat(bigScreenPage.getByText("P1")).isVisible();

            // Check if others are disabled
            assertThat(p2Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GRAY);
            assertThat(p3Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GRAY);

            // And P1 should see "Odpowiadasz"
            assertThat(p1Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GREEN);

            // 9. Players do not answer in application but tell their answers to maestro.
            // Let's assume that this player answered correctly.
            log.info("first: Maestro reporting correct answer for P1");
            // Wait for Maestro to show PlayTimeComponent.
            assertThat(maestroPage.getByText("Odpowiada: P1")).isVisible();

            maestroPage.getByTestId("maestro/dj/play/artist-checkbox-2-1").click();
            maestroPage.getByTestId("maestro/dj/play/title-checkbox-2-1").click();
            maestroPage.getByTestId("maestro/dj/play/confirm-2-1").click();

            // 10. After that the piece is immediately revealed on big screen and players see their points
            log.info("first: Verifying big screen reveal");
            // RevealView uses different text than "No i zgłoś się!"
            assertThat(bigScreenPage.getByText(info.artist)).isVisible();
            assertThat(bigScreenPage.getByText(info.title)).isVisible();

            log.info("first: Verifying player points");
            // FIRST mode points: artist 12, title 16. Total 28.
            // Wait for players to be redirected to PieceResultView
            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("28");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("0");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("0");

            log.info("Big screen revals correct answers");
            assertThat(bigScreenPage.getByText(info.artist)).isVisible();
            assertThat(bigScreenPage.getByText(info.title)).isVisible();

            // 11. Maestro selects the second piece
            log.info("first: Selecting first piece");
            var info2 = expandPiece(maestroPage, 2, 2);
            maestroPage.getByTestId("maestro/dj/piece-PLAY-2-2").click();

            for (var playerPage: List.of(p1Page, p2Page, p3Page)) {
                assertThat(playerPage.getByTestId("player/play")).isVisible();
                Locator theButton = p1Page.getByTestId("player/play/button");
                assertThat(theButton).hasCSS("background-color", Palette.BLUE);
            }


            // 12. No one volunteers - maestro reveal the answer
            log.info("first: revealing second piece");
            maestroPage.getByTestId("maestro/dj/piece-REVEAL-2-2").click();

            for (var playerPage: List.of(p1Page, p2Page, p3Page)) {
                assertThat(playerPage.getByTestId("player/piece-result/points")).hasText("0");
            }

            assertThat(bigScreenPage.getByText(info2.artist)).isVisible();
            assertThat(bigScreenPage.getByText(info2.title)).isVisible();

            // 13. Maestro selects the third piece
            log.info("first: Selecting third piece");
            var info3 = expandPiece(maestroPage, 2, 3);
            maestroPage.getByTestId("maestro/dj/piece-PLAY-2-3").click();

            for (var playerPage: List.of(p1Page, p2Page, p3Page)) {
                assertThat(playerPage.getByTestId("player/play")).isVisible();
                Locator theButton = p1Page.getByTestId("player/play/button");
                assertThat(theButton).hasCSS("background-color", Palette.BLUE);
            }


            // player2 provides both wrong answers
            log.info("player2 provides both wrong answers");

            p2Page.getByTestId("player/play/button").click();
            assertThat(bigScreenPage.getByText("P2")).isVisible();
            assertThat(p1Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GRAY);
            assertThat(p2Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GREEN);
            assertThat(p3Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GRAY);
            maestroPage.getByTestId("maestro/dj/play/confirm-2-3").click();


            assertThat(p1Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.BLUE);
            assertThat(p2Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.RED);
            assertThat(p3Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.BLUE);
            assertThat(bigScreenPage.getByText("No i zgłoś się!")).isVisible();


            log.info("player1 provides correct title (only)");

            p1Page.getByTestId("player/play/button").click();
            assertThat(bigScreenPage.getByText("P1")).isVisible();
            assertThat(p1Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GREEN);
            assertThat(p2Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.RED);
            assertThat(p3Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GRAY);

            maestroPage.getByTestId("maestro/dj/play/title-checkbox-2-3").click();
            maestroPage.getByTestId("maestro/dj/play/confirm-2-3").click();

            assertThat(p1Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.AMBER);
            assertThat(p2Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.RED);
            assertThat(p3Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.BLUE);
            assertThat(bigScreenPage.getByText("No i zgłoś się!")).isVisible();

            log.info("player3 provides correct artist");

            p3Page.getByTestId("player/play/button").click();

            assertThat(p1Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.AMBER);
            assertThat(p2Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.RED);
            assertThat(p3Page.getByTestId("player/play/button")).hasCSS("background-color", Palette.GREEN);

            assertThat(maestroPage.getByTestId("maestro/dj/play/title-checkbox-2-3")).hasAttribute("disabled", "");
            maestroPage.getByTestId("maestro/dj/play/artist-checkbox-2-3").click();
            maestroPage.getByTestId("maestro/dj/play/confirm-2-3").click();

            log.info("the answer is now revealed and palyers see theri points");

            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("32");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("0");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("36");

            assertThat(bigScreenPage.getByText(info3.artist)).isVisible();
            assertThat(bigScreenPage.getByText(info3.title)).isVisible();
        }
    }

    @Test
    void gameInOnionModeFlow() {
        try (BrowserContext maestroContext = browser.newContext();
             BrowserContext bigScreenContext = browser.newContext();
             BrowserContext p1Context = browser.newContext();
             BrowserContext p2Context = browser.newContext();
             BrowserContext p3Context = browser.newContext()) {

            Page maestroPage = maestroContext.newPage();
            Page bigScreenPage = bigScreenContext.newPage();
            Page p1Page = p1Context.newPage();
            Page p2Page = p2Context.newPage();
            Page p3Page = p3Context.newPage();
            List.of(p1Page, p2Page, p3Page).forEach(p -> p.setViewportSize(480, 640));

            // 1. Maestro starts the game
            initTheGame(maestroPage, bigScreenPage);

            // 2. 3 players join
            log.info("onion: Players joining");
            joinPlayer(p1Page, "P1");
            joinPlayer(p2Page, "P2");
            joinPlayer(p3Page, "P3");

            // 3. Maestro starts the third round (ONION mode)
            log.info("onion: Starting third round");
            maestroPage.getByTestId("maestro/dj/round-header-3").click();
            maestroPage.getByTestId("maestro/dj/round-init/activate-3").click();

            // 4. On big screen info about that round is displayed
            log.info("onion: Verifying round info on Big Screen");
            assertThat(bigScreenPage.getByTestId("big-screen/progress-bar/Runda")).containsText("Runda:  3 /");

            // 5. Then maestro selects first piece of round 3
            log.info("onion: Selecting first piece");
            var info = expandPiece(maestroPage, 3, 1);
            maestroPage.getByTestId("maestro/dj/piece-ONION_LISTEN-3-1").click();

            // 6. Users provide answers - Order matters for points in ONION mode
            log.info("onion: Players providing answers (P1 first, P2 second, P3 third)");
            // Multipliers: 1st correct -> 4x, 2nd -> 3x, 3rd -> 3x
            // Round 3 mode: ONION (artist 2 pts, title 3 pts)

            provideAnswer(p1Page, info, info.artist, info.title); // 1st: 2*4 + 3*4 = 8 + 12 = 20
            validateSlackers(bigScreenPage, List.of("P2", "P3"));

            provideAnswer(p2Page, info, info.artist, info.title); // 2nd: 2*3 + 3*3 = 6 + 9 = 15
            validateSlackers(bigScreenPage, List.of("P3"));

            provideAnswer(p3Page, info, info.artist, info.title); // 3rd: 2*3 + 3*3 = 6 + 9 = 15
            validateSlackers(bigScreenPage, List.of());

            // 7. Maestro reveals answers and players see points
            log.info("onion: Maestro revealing answers for 3-1");
            maestroPage.getByTestId("maestro/dj/piece-REVEAL-3-1").click();

            log.info("onion: Verifying points for 3-1");
            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("20");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("15");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("15");

            // 8. Second piece of round 3
            log.info("onion: Selecting second piece");
            var info2 = expandPiece(maestroPage, 3, 2);
            maestroPage.getByTestId("maestro/dj/piece-ONION_LISTEN-3-2").click();

            log.info("onion: Players providing answers (P3 first, P2 second, P1 third, P1 wrong artist)");
            // P3: 1st correct -> 2*4 + 3*4 = 20
            // P2: 2nd correct -> 2*3 + 3*3 = 15
            // P1: 3rd artist wrong, title correct -> 2*0 + 3*3 = 9

            provideAnswer(p3Page, info2, info2.artist, info2.title);
            provideAnswer(p2Page, info2, info2.artist, info2.title);
            provideAnswer(p1Page, info2, "Blur", info2.title);

            log.info("onion: Maestro revealing answers for 3-2");
            maestroPage.getByTestId("maestro/dj/piece-REVEAL-3-2").click();

            log.info("onion: Verifying points for 3-2");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("20");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("15");
            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("9");

            // 8. third piece of round 3
            // the third piece have alternative title, and alternative artist
            log.info("onion: Selecting third piece");
            var info3 = expandPiece(maestroPage, 3, 3);
            maestroPage.getByTestId("maestro/dj/piece-ONION_LISTEN-3-3").click();


            provideAnswer(p3Page, info2, null, info3.title);
            provideAnswer(p2Page, info2, info3.artist, null);
            // p1 provides alternative correct answers
            provideAnswer(p1Page, info2, "SunStroke", "Run Away");

            log.info("onion: Maestro revealing answers for 3-3");
            maestroPage.getByTestId("maestro/dj/piece-REVEAL-3-3").click();

            log.info("onion: Verifying points for 3-3");
            assertThat(p3Page.getByTestId("player/piece-result/points")).hasText("12");
            assertThat(p2Page.getByTestId("player/piece-result/points")).hasText("8");
            assertThat(p1Page.getByTestId("player/piece-result/points")).hasText("15");


            // big screen displays both, base answers and alternative answers
            assertThat(bigScreenPage.getByText(info3.artist)).isVisible();
            assertThat(bigScreenPage.getByText("SunStroke")).isVisible();

            assertThat(bigScreenPage.getByText(info3.title)).isVisible();
            assertThat(bigScreenPage.getByText("Run Away")).isVisible();
        }
    }

    private void validateSlackers(Page bigScreenPage, List<String> slackers) {
        if (slackers.isEmpty()) {
            assertThat(bigScreenPage.getByTestId("big-screen/listen/slackers-none")).isVisible();
        } else {
            assertThat(bigScreenPage.locator("span[class='test-class/big-screen/listen/slacker']")).hasCount(slackers.size());
            var actualSlackers = bigScreenPage.locator("span[class='test-class/big-screen/listen/slacker']").allInnerTexts();
            Assertions.assertThat(actualSlackers).containsExactlyInAnyOrderElementsOf(slackers);
        }
    }

    private PieceInfo expandPiece(Page maestroPage, int round, int piece) {
        maestroPage.getByTestId("maestro/dj/piece-header-" + round + "-" + piece).click();
        return new PieceInfo(
                maestroPage.getByTestId("maestro/dj/piece-artist-" + round + "-" + piece).innerText(),
                maestroPage.getByTestId("maestro/dj/piece-title-" + round + "-" + piece).innerText()
        );
    }

    private void selectValue(Page page, String field, @Nullable String value) {
        if (value != null) {
            page.locator("input[data-testid='player/answer/" + field + "']").fill(value);
            page.locator("li[role='option']").getByText(value, new Locator.GetByTextOptions().setExact(true)).first().click();
        } else {
            page.locator("input[data-testid='player/answer/" + field + "']").click();
            page.getByTestId("player/answer/" + field + "-input-dunno").click();
        }
    }

    private void provideAnswer(Page page, PieceInfo pieceInfo, @Nullable String artistAnswer, @Nullable String titleAnswer) {
        // Wait for AnswerView to appear
        page.waitForURL(url -> url.endsWith("/answer"));
        if (!pieceInfo.artist.equals(Constants.UNKNOWN)) {
            selectValue(page, "artist", artistAnswer);
        }
        selectValue(page, "title", titleAnswer);

        page.getByTestId("player/answer/confirm").click();
    }

    private void initTheGame(Page maestroPage, Page bigScreenPage) {
        ensureGameNotStarted(maestroPage);
        loadBigScreenInvite(bigScreenPage);

        log.info("unique: Selecting game");
        maestroPage.getByTestId("maestro/start/game-selection").click();
        maestroPage.locator("vaadin-combo-box-item >> text=ALL").waitFor();
        maestroPage.locator("vaadin-combo-box-item >> text=ALL").click();

        assertThat(maestroPage.getByTestId("meastro/start/button")).isEnabled();
        maestroPage.getByTestId("meastro/start/button").click();

        log.info("Clicked Start button");
    }

    private void loadBigScreenInvite(Page bigScreenPage) {
        // 2. Big screen shows welcome page
        log.info("Big Screen navigating to /big-screen");
        bigScreenPage.navigate(BASE_URL + "/big-screen");
        bigScreenPage.waitForLoadState();
        assertThat(bigScreenPage.getByTestId("big-screen/top")).isVisible();
        log.info("Big Screen top visible");

        // Wait for the redirect to invite view if it happens
        bigScreenPage.waitForURL(url -> url.endsWith("/invite"), new Page.WaitForURLOptions());
        log.info("Big Screen at /invite");

        assertThat(bigScreenPage.getByText("Czekamy na graczy")).isVisible();
        log.info("'Czekamy na graczy' visible");
    }

    private void ensureGameNotStarted(Page maestroPage) {
        log.info("Ensuring game not started");
        maestroPage.navigate(BASE_URL + "/maestro");
        maestroPage.waitForURL(url -> url.endsWith("/dj") || url.endsWith("start"));

        if (maestroPage.url().endsWith("/dj")) {
            log.info("Game already started, resetting");
            // If the reset button is not visible, it might be inside a collapsed accordion
            if (!maestroPage.getByTestId("maestro/reset/button").isVisible()) {
                log.info("Reset button not visible, expanding wrap-up");
                maestroPage.getByTestId("maestro/dj/wrapup-header").click();
            }
            maestroPage.getByTestId("maestro/reset/danger").click();
            maestroPage.getByTestId("maestro/reset/button").click();
            maestroPage.waitForURL(url -> url.endsWith("start"));
        }
        log.info("Game state: NOT STARTED");
    }

    private void joinPlayer(Page page, String nickname) {
        page.navigate(BASE_URL + "/");
        // RootView redirects to JoinView
        page.locator("vaadin-text-field[data-testid='player/join/nickname'] input").fill(nickname);
        page.getByTestId("player/join/button").click();
        // Should navigate to WaitForOthersView (since game just started)
        assertThat(page.getByText("poczekaj na pozostałych graczy")).isVisible();
    }
}
