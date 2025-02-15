package com.github.mjjaniec.model;

import com.github.mjjaniec.views.bigscreen.BigScreenRoute;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.github.mjjaniec.views.bigscreen.RoundInitView;
import com.github.mjjaniec.views.player.PlayerRoute;
import com.github.mjjaniec.views.player.WaitForOthersView;
import com.github.mjjaniec.views.player.WaitForRoundView;
import com.vaadin.flow.component.Component;

import java.util.List;
import java.util.Optional;

public interface GameStage<PV extends Component & PlayerRoute, BSV extends Component & BigScreenRoute> {

    Class<PV> playerView();

    Class<BSV> bigScreenView();

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
        LISTEN, REPORT;
    }

    record Invite() implements GameStage<WaitForOthersView, InviteView> {

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
                     RoundSummary roundSummary) implements GameStage<WaitForRoundView, RoundInitView> {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> bigScreenView() {
            return RoundInitView.class;
        }

    }

    record RoundPiece(PieceNumber pieceNumber, MainSet.Piece piece,
                      List<PieceStage> pieceStage) implements GameStage<WaitForRoundView, RoundInitView> {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> bigScreenView() {
            return RoundInitView.class;
        }
    }

    record RoundSummary(RoundNumber roundNumber) implements GameStage<WaitForRoundView, RoundInitView> {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> bigScreenView() {
            return RoundInitView.class;
        }

    }

    record WrapUp() implements GameStage<WaitForRoundView, RoundInitView> {
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