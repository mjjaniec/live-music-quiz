package com.github.mjjaniec.model;

import com.github.mjjaniec.views.bigscreen.BigScreenRoute;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.github.mjjaniec.views.bigscreen.RoundInitView;
import com.github.mjjaniec.views.player.*;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

public interface GameStage {

    Class<? extends PlayerRoute> playerView();

    Class<? extends BigScreenRoute> bigScreenView();

    default Optional<Invite> asInvite() {
        return switch (this) {
            case Invite it -> Optional.of(it);
            default -> Optional.empty();
        };
    }

    default Optional<RoundInit> asRoundInit() {
        return switch (this) {
            case RoundInit it -> Optional.of(it);
            default -> Optional.empty();
        };
    }

    record RoundNumber(long number, long of) {
    }

    record PieceNumber(long number, long of) {
    }


    enum PieceStage {
        LISTEN, ANSWER
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

    class RoundPiece implements GameStage {
        public final PieceNumber pieceNumber;
        public final MainSet.Piece piece;
        public final List<PieceStage> innerStages;
        @Setter
        private PieceStage currentStage;

        public RoundPiece(PieceNumber pieceNumber, MainSet.Piece piece, List<PieceStage> innerStages) {
            this.pieceNumber = pieceNumber;
            this.piece = piece;
            this.currentStage = innerStages.getFirst();
            this.innerStages = innerStages;
        }

        @Override
        public Class<? extends PlayerRoute> playerView() {
            return switch (currentStage) {
                case LISTEN -> ListenView.class;
                case ANSWER -> AnswerView.class;
            };
        }

        @Override
        public Class<RoundInitView> bigScreenView() {
            return RoundInitView.class;
        }
    }

    record RoundSummary(RoundNumber roundNumber) implements GameStage {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> bigScreenView() {
            return RoundInitView.class;
        }

    }

    record WrapUp() implements GameStage {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> bigScreenView() {
            return RoundInitView.class;
        }

    }
}