package com.github.mjjaniec.util;

import com.github.mjjaniec.model.Player;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import jakarta.servlet.http.Cookie;
import org.atmosphere.util.CookieUtil;

import java.util.Arrays;
import java.util.Optional;

public class Cookies {
    private static final String USERNAME = "lmq-username";
    private static final int USERNAME_MAX_AGE_SEC = 4 * 60 * 60;    // 4h

    private static String encode(String name) {
        return name.replaceAll(" ", "_space_");
    }

    private static String decode(String cookieName) {
        return cookieName.replaceAll("_space_" , " ");
    }

    public static Optional<Player> readPlayer() {
        return Arrays.stream(VaadinRequest.getCurrent().getCookies())
                .filter(cookie -> USERNAME.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .map(Cookies::decode)
                .map(Player::new);
    }

    public static void savePlayer(Player player) {
        Cookie playerCookie = new Cookie(USERNAME, encode(player.name()));
        playerCookie.setMaxAge(USERNAME_MAX_AGE_SEC);
        playerCookie.setPath(VaadinRequest.getCurrent().getContextPath());
        VaadinResponse.getCurrent().addCookie(playerCookie);
    }

    public static void removePlayer() {
        Cookie playerCookie = new Cookie(USERNAME, "");
        playerCookie.setMaxAge(0);
        playerCookie.setPath(VaadinRequest.getCurrent().getContextPath());
        VaadinResponse.getCurrent().addCookie(playerCookie);
    }
}
