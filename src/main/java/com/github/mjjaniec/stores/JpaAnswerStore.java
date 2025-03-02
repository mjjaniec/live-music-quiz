package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Answer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public interface JpaAnswerStore extends CrudRepository<AnswerDto, String>, AnswerStore  {
    @Override
    default void saveAnswer(Answer answer) {
        save(mapToDto(answer));
    }

    @Override
    default Stream<Answer> playerAnswers(String player, int round) {
        return findByPlayerAndRound(player, round).stream().map(this::mapFromDto);
    }

    @Override
    default void clearAnswers() {
        deleteAll();
    }

    @Override
    default Optional<Answer> playerAnswer(String player, int round, int piece) {
        return findById(id(player, round, piece)).map(this::mapFromDto);
    }

    List<AnswerDto> findByPlayerAndRound(String player, int round);

    private Answer mapFromDto(AnswerDto dto) {
        return new Answer(dto.isArtist(), dto.isTitle(), dto.getBonus(), dto.getPlayer(), dto.getRound(), dto.getPiece());
    }

    private AnswerDto mapToDto(Answer answer) {
        AnswerDto dto = new AnswerDto();
        dto.setId(id(answer.player(), answer.round(), answer.piece()));
        dto.setArtist(answer.artist());
        dto.setTitle(answer.title());
        dto.setBonus(answer.bonus());
        dto.setPlayer(answer.player());
        dto.setRound(answer.round());
        dto.setPiece(answer.piece());
        return dto;
    }

    private String id(String player, int round, int piece) {
        return round + ":" + piece + ":" + player;
    }

}
