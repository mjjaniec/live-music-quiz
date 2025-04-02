package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.config.ApplicationConfig;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig.class)
@Theme(value = "live-music-quiz")
@Push
public class LiveMusicQuizApp implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", "themes/live-music-quiz/favico.svg", "128x128");
    }

    public static void main(String[] args) throws IOException {
        SpringApplication.run(LiveMusicQuizApp.class, args);
    }
}

