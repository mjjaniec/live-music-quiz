package com.github.mjjaniec.lmq.model;

import com.github.mjjaniec.lmq.views.bigscreen.*;
import com.github.mjjaniec.lmq.views.player.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public sealed interface GameStage {

    Class<? extends PlayerRoute> playerView();

    Class<? extends BigScreenRoute> bigScreenView();

    record RoundNumber(int number, int of) {
    }

    record PieceNumber(int number, int of) {
    }


    enum PieceStage {
        LISTEN, ONION_LISTEN, REVEAL, PLAY
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
                     MainSet.RoundMode roundMode,
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
        private boolean bonus;
        @Getter
        private final List<String> failedResponders;
        @Setter
        @Getter
        @Nullable
        private String currentResponder;
        @Getter
        @Setter
        private int artistAnswered;
        @Getter
        @Setter
        private int titleAnswered;

        public boolean isCompleted() {
            return artistAnswered != 0 && titleAnswered != 0;
        }

        public RoundPiece(int roundNumber, PieceNumber pieceNumber, MainSet.Piece piece, List<PieceStage> innerStages) {
            this.roundNumber = roundNumber;
            this.pieceNumber = pieceNumber;
            this.piece = piece;
            this.currentStage = innerStages.getFirst();
            this.innerStages = innerStages;
            this.bonus = false;
            this.artistAnswered = 0;
            this.titleAnswered = 0;
            failedResponders = new ArrayList<>();
        }

        public void incrementArtistAnswered() {
            ++this.artistAnswered;
        }

        public void incrementTitleAnswered() {
            ++this.titleAnswered;
        }

        public void addFailedResponder(String responder) {
            this.failedResponders.add(responder);
            this.currentResponder = null;
        }

        @Override
        public Class<? extends PlayerRoute> playerView() {
            return switch (currentStage) {
                case LISTEN, ONION_LISTEN -> AnswerView.class;
                case REVEAL -> PieceResultView.class;
                case PLAY -> PlayView.class;
            };
        }

        @Override
        public Class<? extends BigScreenRoute> bigScreenView() {
            return switch (currentStage) {
                case LISTEN, ONION_LISTEN -> BigScreenListenView.class;
                case REVEAL -> RevealView.class;
                case PLAY -> BigScreenPlayView.class;
            };
        }

        public void clear() {
            bonus = false;
            artistAnswered = 0;
            titleAnswered = 0;
            failedResponders.clear();
            currentResponder = null;
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
        private Integer showFrom;

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