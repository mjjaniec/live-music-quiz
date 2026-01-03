package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.model.Constants;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
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

            // TODO continue
//            // 7. Users provide answers
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
            assertThat(p1Page.getByTestId("player/piece-result/points")).isVisible();
            assertThat(p2Page.getByTestId("player/piece-result/points")).isVisible();
            assertThat(p3Page.getByTestId("player/piece-result/points")).isVisible();

            // assrt that
        }
    }

    private void validateSlackers(Page bigScreenPage, List<String> slackers) {
        if (slackers.isEmpty()) {
            bigScreenPage.getByTestId("big-screen/listen/slackers-none").isVisible();
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
            page.locator("li[role='option']").getByText(value).click();
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
