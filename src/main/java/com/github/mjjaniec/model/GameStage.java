package com.github.mjjaniec.model;

import com.github.mjjaniec.views.bigscreen.BigScreenRoute;
import com.github.mjjaniec.views.bigscreen.InviteView;
import com.github.mjjaniec.views.bigscreen.RoundInitView;
import com.github.mjjaniec.views.player.PlayerRoute;
import com.github.mjjaniec.views.player.RootView;
import com.github.mjjaniec.views.player.WaitForRoundView;
import com.vaadin.flow.component.Component;

import java.util.Map;
import java.util.Optional;

public interface GameStage<PV extends Component & PlayerRoute, BSV extends Component & BigScreenRoute> {

    Class<PV> playerView();

    Class<BSV> getBigScreenView();

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

    record RoundNumber(int number, int of) {
    }

    record PieceNumber(int number, int of) {
    }


    enum PieceStage {
        LISTEN, REPORT;
    }

    record Invite() implements GameStage<RootView, InviteView> {

        @Override
        public Class<RootView> playerView() {
            return RootView.class;
        }

        @Override
        public Class<InviteView> getBigScreenView() {
            return InviteView.class;
        }
    }

    record RoundInit(RoundNumber roundNumber,
                     MainSet.RoundMode mode) implements GameStage<WaitForRoundView, RoundInitView> {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> getBigScreenView() {
            return RoundInitView.class;
        }
    }

    record RoundPiece(RoundNumber roundNumber, PieceNumber pieceNumber, MainSet.Piece piece,
                      PieceStage pieceStage) implements GameStage<WaitForRoundView, RoundInitView> {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> getBigScreenView() {
            return RoundInitView.class;
        }
    }

    record RoundSummary(RoundNumber roundNumber, MainSet.RoundMode mode,
                        Map<Integer, Map<Player, Integer>> roundToPlayerToPoints) implements GameStage<WaitForRoundView, RoundInitView> {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> getBigScreenView() {
            return RoundInitView.class;
        }
    }

    record WrapUp() implements GameStage<WaitForRoundView, RoundInitView> {
        @Override
        public Class<WaitForRoundView> playerView() {
            return WaitForRoundView.class;
        }

        @Override
        public Class<RoundInitView> getBigScreenView() {
            return RoundInitView.class;
        }
    }
}