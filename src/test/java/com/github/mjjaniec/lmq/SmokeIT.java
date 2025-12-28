package com.github.mjjaniec.lmq;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmokeIT {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        // Set headless to true for CI/CD environments
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
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
        page.navigate("http://localhost:8080/");

        page.waitForLoadState();
        assertTrue(page.title().contains("Live Music Quiz"));
        assertThat(page.getByTestId("player/join/button")).isVisible();
    }

    @Test
    void maestroPageLoads() {
        page.navigate("http://localhost:8080/maestro/start");
        page.waitForLoadState();

        assertTrue(page.title().contains("Live Music Quiz"));
        assertThat(page.getByTestId("meastro/start/button")).isVisible();
    }

    @Test
    void bigscreenPageLoads() {
        page.navigate("http://localhost:8080/maestro/start");
        page.waitForLoadState();

        assertTrue(page.title().contains("Live Music Quiz"));
        assertThat(page.getByTestId("meastro/start/button")).isVisible();
    }
}
