package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.config.ApplicationConfig;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig.class)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("themes/live-music-quiz/styles.css")
@Push
public class LiveMusicQuizApp implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", "themes/live-music-quiz/favico.svg", "128x128");
    }

    static void main(String[] args) {
        SpringApplication.run(LiveMusicQuizApp.class, args);
    }
}

