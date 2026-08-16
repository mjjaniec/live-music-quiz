package com.github.mjjaniec.lmq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/**
 * Guards {@code SecurityConfig}'s {@code permitAll} rule ordering: it must keep winning over
 * {@code VaadinSecurityConfigurer}'s catch-all {@code denyAll}, or these no-session requests would
 * start getting redirected to the login page instead of served directly.
 */
class SecurityConfigIT {

    private static final int PORT = Integer.parseInt(System.getProperty("server.port", "8090"));
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static final HttpClient CLIENT =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Test
    void testLoginIsReachableWithoutSession() throws IOException, InterruptedException {
        assertEquals(200, get("/test/login?email=" + TestAuth.MAESTRO_EMAIL).statusCode());
    }

    @Test
    void hintEndpointsAreReachableWithoutSession() throws IOException, InterruptedException {
        assertEquals(200, get("/api/v1/hint/artist").statusCode());
        assertEquals(200, get("/api/v1/hint/title").statusCode());
    }

    private HttpResponse<Void> get(String path) throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
    }
}
