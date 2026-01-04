package com.github.mjjaniec.lmq;

import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.github.mjjaniec.lmq.services.Navigator;
import com.github.mjjaniec.lmq.services.Results;
import com.github.mjjaniec.lmq.stores.AnswerStore;
import com.github.mjjaniec.lmq.stores.PlayerStore;
import com.github.mjjaniec.lmq.stores.QuizStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public class WrapUpDataInjectionIT {

    @Autowired
    private MaestroInterface gameService;

    @Autowired
    private PlayerStore playerStore;

    @Autowired
    private AnswerStore answerStore;

    @Autowired
    private QuizStore quizStore;

    @MockitoBean
    private SpreadsheetLoader spreadsheetLoader;

    @MockitoBean
    private Navigator navigator;

    @TestConfiguration
    static class Config {
        @Bean
        public SpreadsheetLoader spreadsheetLoader() {
            SpreadsheetLoader mock = Mockito.mock(SpreadsheetLoader.class);
            Mockito.when(mock.loadPlayOffs()).thenReturn(new PlayOffs(List.of()));
            return mock;
        }
    }

    @BeforeEach
    void setup() {
        Mockito.when(spreadsheetLoader.loadPlayOffs()).thenReturn(new PlayOffs(List.of()));
        playerStore.clearPlayers();
        answerStore.clearAnswers();
        quizStore.clearQuiz();
    }

    @Test
    void injectDataAndVerifyResults() {
        // 1. Setup a minimal game structure
        MainSet.Piece piece1 = new MainSet.Piece("Artist1", null, "Title1", null, null, null, Set.of("ALL"));
        MainSet.LevelPieces level1 = new MainSet.LevelPieces(MainSet.RoundMode.EVERYBODY, List.of(piece1));
        MainSet mainSet = new MainSet(List.of(level1));

        gameService.reset();
        gameService.initGame(mainSet);

        // 2. Inject players
        playerStore.clearPlayers();
        playerStore.addPlayer("Alice");
        playerStore.addPlayer("Bob");
        playerStore.addPlayer("Charlie");

        // 3. Inject answers
        // Round 1 mode: EVERYBODY (Artist: 4 pts, Title: 6 pts)
        answerStore.clearAnswers();
        // Alice: Correct Artist & Title = 10 pts
        answerStore.saveAnswer(new Answer(true, true, 10, "Alice", 1, 1, "Artist1", "Title1"));
        // Bob: Correct Artist only = 4 pts
        answerStore.saveAnswer(new Answer(true, false, 4, "Bob", 1, 1, "Artist1", null));
        // Charlie: Wrong Artist & Title = 0 pts
        answerStore.saveAnswer(new Answer(false, false, 0, "Charlie", 1, 1, null, null));

        // 4. Navigate to WrapUp
        StageSet stageSet = gameService.stageSet();
        assertThat(stageSet).isNotNull();
        gameService.setStage(stageSet.wrapUpStage());

        // 5. Verify results
        Results results = gameService.results();
        assertThat(results.rows()).hasSize(3);

        // Ranking should be: Alice (10), Bob (4), Charlie (0)
        Results.Row aliceRow = results.rows().stream().filter(r -> r.player().equals("Alice")).findFirst().orElseThrow();
        assertThat(aliceRow.total()).isEqualTo(10);
        assertThat(aliceRow.position()).isEqualTo(1);
        assertThat(aliceRow.award()).contains(Results.Award.FIRST);

        Results.Row bobRow = results.rows().stream().filter(r -> r.player().equals("Bob")).findFirst().orElseThrow();
        assertThat(bobRow.total()).isEqualTo(4);
        assertThat(bobRow.position()).isEqualTo(2);
        assertThat(bobRow.award()).contains(Results.Award.SECOND);

        Results.Row charlieRow = results.rows().stream().filter(r -> r.player().equals("Charlie")).findFirst().orElseThrow();
        assertThat(charlieRow.total()).isEqualTo(0);
        assertThat(charlieRow.position()).isEqualTo(3);
        assertThat(charlieRow.award()).contains(Results.Award.THIRD);
    }
}
