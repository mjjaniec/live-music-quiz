package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.StageSet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public interface JpaStageStore extends CrudRepository<StageDto, Long>, StageStore {
    @Override
    default Optional<GameStage> readStage(StageSet stageSet) {
        Iterator<StageDto> result = findAll().iterator();
        if (result.hasNext()) {
            return Optional.of(fromDto(result.next(), stageSet)).flatMap(Function.identity());
        } else {
            return Optional.empty();
        }
    }

    @Override
    default void saveStage(GameStage stage) {
        deleteAll();
        save(toDto(stage));
    }

    @Override
    default void clearStage() {
        deleteAll();
    }

    private Optional<? extends GameStage> fromDto(StageDto dto, StageSet stages) {
        if (dto.getRound() == StageDto.init) {
            return Optional.of(stages.initStage());
        } else if (dto.getRound() == StageDto.summary) {
            return Optional.of(stages.wrapUpStage());
        } else {
            Optional<GameStage.RoundInit> init = stages.roundInit(dto.getRound());
            if (dto.getPiece() == StageDto.init) {
                return init;
            } else if (dto.getPiece() == StageDto.summary) {
                return init.map(GameStage.RoundInit::roundSummary);
            } else {
                return init.map(i -> setUpAdditions(i.pieces().get(dto.getPiece() - 1), dto.getAdditions()));
            }
        }
    }

    private StageDto toDto(GameStage stage) {
        StageDto result = new StageDto();
        switch (stage) {
            case GameStage.Invite ignored -> result.set(StageDto.init, 0);
            case GameStage.RoundInit roundInit -> result.set(roundInit.roundNumber().number(), StageDto.init);
            case GameStage.RoundPiece roundPiece ->
                    result.set(roundPiece.roundNumber, roundPiece.pieceNumber.number(), toPieceAdditions(roundPiece));
            case GameStage.RoundSummary roundSummary ->
                    result.set(roundSummary.roundNumber().number(), StageDto.summary);
            case GameStage.WrapUp ignored -> result.set(StageDto.summary, 0);
        }
        return result;
    }

    private GameStage.RoundPiece setUpAdditions(GameStage.RoundPiece piece, String additions) {
        String[] a = additions.split(":");
        piece.setBonus(Integer.parseInt(a[0]));
        piece.setCurrentStage(GameStage.PieceStage.valueOf(a[1]));
        if (a.length >= 3) {
            piece.setCurrentResponder(a[2]);
        }
        if (a.length >= 4) {
            piece.setFailedResponders(Arrays.stream(a, 3, a.length).toList());
        }
        return piece;
    }

    private String toPieceAdditions(GameStage.RoundPiece piece) {
        return Stream.of(
                        Stream.of(String.valueOf(piece.getBonus()), piece.getCurrentStage().name()),
                        Optional.ofNullable(piece.getCurrentResponder()).stream(),
                        piece.getFailedResponders().stream()
                ).flatMap(Function.identity())
                .collect(Collectors.joining(":"));
    }
}
