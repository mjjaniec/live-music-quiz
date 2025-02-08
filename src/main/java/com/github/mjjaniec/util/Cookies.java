package com.github.mjjaniec.util;

import com.github.mjjaniec.model.Player;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.Optional;

public class Cookies {
    private static final String USERNAME = "lmq-username";
    private static final int USERNAME_MAX_AGE_SEC = 4 * 60 * 60;    // 4h

    public static Optional<Player> readPlayer() {
        return Arrays.stream(VaadinRequest.getCurrent().getCookies())
                .filter(cookie -> USERNAME.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .map(Player::new);
    }

    public static void savePlayer(Player player) {
        Cookie playerCookie = new Cookie(USERNAME, player.name());
        playerCookie.setMaxAge(USERNAME_MAX_AGE_SEC);
        playerCookie.setPath(VaadinRequest.getCurrent().getContextPath());
        VaadinResponse.getCurrent().addCookie(playerCookie);
    }
}
