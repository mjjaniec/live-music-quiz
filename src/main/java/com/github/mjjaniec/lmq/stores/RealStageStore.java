package com.github.mjjaniec.lmq.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.model.StageSet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RealStageStore implements StageStore {

    private final JpaStageGenStore jpaStore;
    private final ObjectMapper mapper;

    private record PieceAdditionsDto(GameStage.PieceStage stage, int bonus, String currentResponder,
                                     List<String> failedResponders, boolean artistAnswered, boolean titleAnswered) {
    }

    private record PlayOffDto(boolean performed) {
    }


    @Override
    public Optional<GameStage> readStage(StageSet stageSet) {
        Iterator<StageDto> result = jpaStore.findAll().iterator();
        if (result.hasNext()) {
            return Optional.of(fromDto(result.next(), stageSet)).flatMap(Function.identity());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void saveStage(GameStage stage) {
        jpaStore.deleteAll();
        jpaStore.save(toDto(stage));
    }

    @Override
    public void clearStage() {
        jpaStore.deleteAll();
    }

    private Optional<? extends GameStage> fromDto(StageDto dto, StageSet stages) {
        if (dto.getRound() == StageDto.init) {
            return Optional.of(stages.initStage());
        } else if (dto.getRound() == StageDto.summary) {
            return Optional.of(setUpAdditions(stages.wrapUpStage(), dto.getAdditions()));
        } else if (dto.getRound() == StageDto.playOff) {
            return Optional.of(setUpAdditions(stages.playOff(), dto.getAdditions()));
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
                    result.set(roundPiece.roundNumber, roundPiece.pieceNumber.number(), toAdditions(roundPiece));
            case GameStage.RoundSummary roundSummary ->
                    result.set(roundSummary.roundNumber().number(), StageDto.summary);
            case GameStage.PlayOff playOff -> result.set(StageDto.playOff, 0, toAdditions(playOff));
            case GameStage.WrapUp wrapUp -> result.set(StageDto.summary, 0, toAdditions(wrapUp));
        }
        return result;
    }

    @SneakyThrows
    private GameStage.WrapUp setUpAdditions(GameStage.WrapUp wrapUp, String additions) {
        try {
            wrapUp.setDisplay(GameStage.Display.valueOf(additions));
        } catch (Exception e) {
            wrapUp.setDisplay(null);
        }
        return wrapUp;
    }

    @SneakyThrows
    private String toAdditions(GameStage.WrapUp wrapUp) {
        return wrapUp.getDisplay() != null ? wrapUp.getDisplay().name() : null;
    }

    @SneakyThrows
    private GameStage.PlayOff setUpAdditions(GameStage.PlayOff playOff, String additions) {
        var dto = mapper.readValue(additions, PlayOffDto.class);
        playOff.setPerformed(dto.performed);
        return playOff;
    }

    @SneakyThrows
    private String toAdditions(GameStage.PlayOff playOff) {
        PlayOffDto dto = new PlayOffDto(playOff.isPerformed());
        return mapper.writeValueAsString(dto);
    }

    @SneakyThrows
    private GameStage.RoundPiece setUpAdditions(GameStage.RoundPiece piece, String additions) {
        var dto = mapper.readValue(additions, PieceAdditionsDto.class);
        piece.setCurrentStage(dto.stage);
        piece.setBonus(dto.bonus);
        piece.setCurrentResponder(dto.currentResponder);
        dto.failedResponders.forEach(responder -> piece.getFailedResponders().add(responder));
        piece.setArtistAnswered(dto.artistAnswered);
        piece.setTitleAnswered(dto.titleAnswered);
        return piece;
    }

    @SneakyThrows
    private String toAdditions(GameStage.RoundPiece piece) {
        var dto = new PieceAdditionsDto(piece.getCurrentStage(), piece.getBonus(), piece.getCurrentResponder(), piece.getFailedResponders(), piece.isArtistAnswered(), piece.isTitleAnswered());
        return mapper.writeValueAsString(dto);
    }
}
