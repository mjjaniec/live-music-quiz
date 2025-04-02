package com.github.mjjaniec.lmq.model;

import com.github.mjjaniec.lmq.views.bigscreen.*;
import com.github.mjjaniec.lmq.views.player.*;
import com.github.mjjaniec.views.bigscreen.*;
import com.github.mjjaniec.views.player.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public sealed interface GameStage {

    Class<? extends PlayerRoute> playerView();

    Class<? extends BigScreenRoute> bigScreenView();

    default Optional<RoundInit> asRoundInit() {
        return switch (this) {
            case RoundInit it -> Optional.of(it);
            default -> Optional.empty();
        };
    }

    default Optional<RoundPiece> asPiece() {
        return switch (this) {
            case RoundPiece it -> Optional.of(it);
            default -> Optional.empty();
        };
    }

    default Optional<WrapUp> asWrapUp() {
        return switch (this) {
            case WrapUp it -> Optional.of(it);
            default -> Optional.empty();
        };
    }

    default Optional<PlayOff> asPlayOff() {
        return switch (this) {
            case PlayOff it -> Optional.of(it);
            default -> Optional.empty();
        };
    }

    default Optional<RoundSummary> asRoundSummary() {
        return switch (this) {
            case RoundSummary it -> Optional.of(it);
            default -> Optional.empty();
        };
    }

    record RoundNumber(int number, int of) {
    }

    record PieceNumber(int number, int of) {
    }


    enum PieceStage {
        LISTEN, ANSWER, PLAY
    }

    record Invite() implements GameStage {

        @Override
        public Class<WaitForOthersView> playerView() {
            return WaitForOthersView.class;
        }

        @Override
        public Class<InviteView> bigScreenView() {
            return InviteView.class;
        }

    }

    record RoundInit(RoundNumber roundNumber,
                     MainSet.Difficulty difficulty,
                     List<RoundPiece> pieces,
                     RoundSummary roundSummary) implements GameStage {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> bigScreenView() {
            return RoundInitView.class;
        }

    }

    final class RoundPiece implements GameStage {
        public final int roundNumber;
        public final PieceNumber pieceNumber;
        public final MainSet.Piece piece;
        public final List<PieceStage> innerStages;
        @Setter
        @Getter
        private PieceStage currentStage;
        @Setter
        @Getter
        private int bonus;
        @Getter
        private final List<String> failedResponders;
        @Setter
        @Getter
        @Nullable
        private String currentResponder;
        @Getter
        @Setter
        private boolean artistAnswered;
        @Getter
        @Setter
        private boolean titleAnswered;

        public boolean isCompleted() {
            return artistAnswered && titleAnswered;
        }

        public RoundPiece(int roundNumber, PieceNumber pieceNumber, MainSet.Piece piece, List<PieceStage> innerStages) {
            this.roundNumber = roundNumber;
            this.pieceNumber = pieceNumber;
            this.piece = piece;
            this.currentStage = innerStages.getFirst();
            this.innerStages = innerStages;
            this.bonus = 1;
            this.artistAnswered = false;
            this.titleAnswered = false;
            failedResponders = new ArrayList<>();
        }

        public void addFailedResponder(String responder) {
            this.failedResponders.add(responder);
            this.currentResponder = null;
        }

        @Override
        public Class<? extends PlayerRoute> playerView() {
            return switch (currentStage) {
                case LISTEN -> ListenView.class;
                case ANSWER -> AnswerView.class;
                case PLAY -> PlayView.class;
            };
        }

        @Override
        public Class<? extends BigScreenRoute> bigScreenView() {
            return switch (currentStage) {
                case LISTEN -> BigScreenListenView.class;
                case ANSWER -> RevealView.class;
                case PLAY -> BigScreenPlayView.class;
            };
        }
    }

    record RoundSummary(RoundNumber roundNumber) implements GameStage {
        @Override
        public Class<RoundResultView> playerView() {
            return RoundResultView.class;
        }

        @Override
        public Class<RoundSummaryView> bigScreenView() {
            return RoundSummaryView.class;
        }

    }

    @RequiredArgsConstructor
    enum Display {
        SIXTH(true, 6),
        FIFTIETH(true, 5),
        FOURTH(true, 4),
        EMPTY_PODIUM(false, 4),
        THIRD_PODIUM(false, 3),
        SECOND_PODIUM(false, 2),
        FULL_PODIUM(false, 1),
        FULL_TABLE(true, 1);
        public final boolean table;
        public final int showFrom;
    }

    @Getter
    @Setter
    final class PlayOff implements GameStage {

        boolean performed;

        @Override
        public Class<PlayOffView> playerView() {
            return PlayOffView.class;
        }

        @Override
        public Class<BigScreenPlayOffView> bigScreenView() {
            return BigScreenPlayOffView.class;
        }
    }

    @Setter
    @Getter
    final class WrapUp implements GameStage {
        @Nullable
        private Display display = Display.SIXTH;

        @Override
        public Class<FeedbackView> playerView() {
            return FeedbackView.class;
        }

        @Override
        public Class<WrapUpView> bigScreenView() {
            return WrapUpView.class;
        }

    }
}