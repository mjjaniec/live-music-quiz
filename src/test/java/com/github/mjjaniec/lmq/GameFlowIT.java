package com.github.mjjaniec.lmq;

import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Slf4j
public class GameFlowIT {
    private static final int PORT = Integer.parseInt(System.getProperty("server.port", "8090"));
    private static final String BASE_URL = "http://localhost:" + PORT;

    static Playwright playwright;
    static Browser browser;

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
            // Wait for constructor call if needed, or just reload to be sure
            bigScreenPage.reload();
            bigScreenPage.waitForLoadState();
            assertThat(bigScreenPage.getByTestId("big-screen/custom-message-container")).isVisible();
            assertThat(bigScreenPage.getByTestId("big-screen/custom-message")).hasText(testMessage);

            // 4. Clear the message
            log.info("Cleaning public message");
            maestroPage.locator("vaadin-text-field[data-testid='maestro/dj/message-field'] input").fill("");
            // Wait for button to change text to "Wyczyść"
            assertThat(maestroPage.getByTestId("maestro/dj/message-button")).hasText("Wyczyść");
            maestroPage.getByTestId("maestro/dj/message-button").click();

            // 5. Verify logo (or at least that message is gone)
            log.info("Verifying message is gone from Big Screen");
            bigScreenPage.reload();
            bigScreenPage.waitForLoadState();
            assertThat(bigScreenPage.getByTestId("big-screen/custom-message-container")).isHidden();
        }
    }

    private void initTheGame(Page maestroPage, Page bigScreenPage) {
        ensureGameNotStarted(maestroPage);

        log.info("unique: Selecting game");
        maestroPage.getByTestId("maestro/start/game-selection").click();
        maestroPage.locator("vaadin-combo-box-item >> text=ALL").waitFor();
        maestroPage.locator("vaadin-combo-box-item >> text=ALL").click();

        assertThat(maestroPage.getByTestId("meastro/start/button")).isEnabled();
        maestroPage.getByTestId("meastro/start/button").click();

        log.info("Clicked Start button");

        loadBigScreenInvite(bigScreenPage);
    }

    private void loadBigScreenInvite(Page bigScreenPage) {
        // 2. Big screen shows welcome page
        log.info("Big Screen navigating to /big-screen");
        bigScreenPage.navigate(BASE_URL + "/big-screen");
        bigScreenPage.waitForLoadState();
        assertThat(bigScreenPage.getByTestId("big-screen/top")).isVisible();
        log.info("Big Screen top visible");

        // Wait for the redirect to invite view if it happens
        bigScreenPage.waitForURL(url -> url.contains("/invite"), new Page.WaitForURLOptions().setTimeout(15000));
        log.info("Big Screen at /invite");

        assertThat(bigScreenPage.getByText("Czekamy na graczy")).isVisible();
        log.info("'Czekamy na graczy' visible");
    }

    private void ensureGameNotStarted(Page maestroPage) {
        log.info("Ensuring game not started");
        maestroPage.navigate(BASE_URL + "/maestro/start");
        maestroPage.waitForLoadState();
        // Wait for either the start button or the reset button to be attached
        try {
            maestroPage.locator("[data-testid='meastro/start/button'], [data-testid='maestro/reset/button']").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (Exception e) {
            log.warn("Timeout waiting for maestro buttons, current URL: {}", maestroPage.url());
        }

        if (maestroPage.url().contains("/dj") || maestroPage.getByTestId("maestro/reset/button").count() > 0) {
            log.info("Game already started, resetting");
            // If the reset button is not visible, it might be inside a collapsed accordion
            if (!maestroPage.getByTestId("maestro/reset/button").isVisible()) {
                log.info("Reset button not visible, clicking 'Podsumowanie' accordion");
                maestroPage.getByText("Podsumowanie").last().click();
            }
            maestroPage.getByTestId("maestro/reset/danger").click();
            maestroPage.getByTestId("maestro/reset/button").click();
            maestroPage.waitForURL("**/start");
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
