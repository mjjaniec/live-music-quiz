package com.github.mjjaniec.lmq;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmokeIT {
    private static final int PORT = Integer.parseInt(System.getProperty("server.port", "8090"));
    private static final String BASE_URL = "http://localhost:" + PORT;

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        // Set headless to true for CI/CD environments
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void playerPageLoads() {
        page.navigate(BASE_URL + "/");

        page.waitForLoadState();
        assertTrue(page.title().contains("Live Music Quiz"));
        assertThat(page.getByTestId("player/join/button")).isVisible();
    }

    @Test
    void maestroPageLoads() {
        page.navigate(BASE_URL + "/maestro/start");
        page.waitForLoadState();

        assertTrue(page.title().contains("Live Music Quiz"));
        assertThat(page.getByTestId("meastro/start/button")).isVisible();
    }

    @Test
    void bigscreenPageLoads() {
        page.navigate(BASE_URL + "/big-screen");
        page.waitForLoadState();

        assertTrue(page.title().contains("Live Music Quiz"));
        assertThat(page.getByTestId("big-screen/top")).isVisible();
    }
}
