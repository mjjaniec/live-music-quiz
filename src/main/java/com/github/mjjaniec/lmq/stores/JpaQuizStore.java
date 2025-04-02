package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.MainSet;
import org.springframework.data.repository.CrudRepository;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public interface JpaQuizStore extends CrudRepository<QuizDto, Long>, QuizStore {
    @Override
    default void clearQuiz() {
        deleteAll();
    }

    @Override
    default Optional<MainSet> getQuiz() {
        Iterator<QuizDto> result = findAll().iterator();
        if (result.hasNext()) {
            return Optional.of(result.next()).map(this::mapFromDto);
        } else {
            return Optional.empty();
        }
    }

    @Override
    default void setQuiz(MainSet set) {
        save(mapToDto(set));
    }

    private QuizDto mapToDto(MainSet mainSet) {
        QuizDto result = new QuizDto();
        result.setLevels(mainSet.levels().stream().map(this::mapToDto).toList());
        return result;
    }

    private QuizDto.Level mapToDto(MainSet.LevelPieces level) {
        QuizDto.Level res = new QuizDto.Level();
        res.setDifficulty(level.level().name());
        res.setPieces(level.pieces().stream().map(this::mapToDto).toList());
        return res;
    }

    private QuizDto.Piece mapToDto(MainSet.Piece piece) {
        QuizDto.Piece res = new QuizDto.Piece();
        res.setArtist(piece.artist());
        res.setTitle(piece.title());
        res.setInstrument(piece.instrument().name());
        res.setTempo(piece.tempo());
        res.setHint(piece.hint());
        return res;
    }

    private MainSet mapFromDto(QuizDto mainSet) {
        return new MainSet(mainSet.getLevels().stream().map(this::mapFromDto).toList());
    }

    private MainSet.LevelPieces mapFromDto(QuizDto.Level level) {
        return new MainSet.LevelPieces(
                MainSet.Difficulty.valueOf(level.getDifficulty()),
                level.getPieces().stream().map(this::mapFromDto).toList()
        );
    }

    private MainSet.Piece mapFromDto(QuizDto.Piece piece) {
        return new MainSet.Piece(
                piece.getArtist(),
                piece.getTitle(),
                MainSet.Instrument.valueOf(piece.getInstrument()),
                piece.getTempo(),
                piece.getHint(),
                Set.of()
        );
    }
}
