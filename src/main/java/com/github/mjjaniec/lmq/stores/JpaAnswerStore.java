package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Answer;
import com.google.common.collect.Streams;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public interface JpaAnswerStore extends CrudRepository<AnswerDto, String>, AnswerStore {
    @Override
    default void saveAnswer(Answer answer) {
        save(mapToDto(answer));
    }

    @Override
    default Stream<Answer> playerAnswers(String player, int round) {
        return findByPlayerAndRound(player, round).stream().map(this::mapFromDto);
    }

    @Override
    default Stream<Answer> allAnswers() {
        return Streams.stream(findAll()).map(this::mapFromDto);
    }

    @Override
    default void clearAnswers() {
        deleteAll();
    }

    @Override
    default void deleteAnswer(String player, int round, int piece) {
        deleteById(id(player, round, piece));
    }

    @Override
    default Optional<Answer> playerAnswer(String player, int round, int piece) {
        return findById(id(player, round, piece)).map(this::mapFromDto);
    }

    List<AnswerDto> findByPlayerAndRound(String player, int round);

    private Answer mapFromDto(AnswerDto dto) {
        return new Answer(dto.isArtist(), dto.isTitle(), dto.getPoints(), dto.getPlayer(), dto.getRound(), dto.getPiece(), dto.getActualArtist(), dto.getActualTitle());
    }

    private AnswerDto mapToDto(Answer answer) {
        AnswerDto dto = new AnswerDto();
        dto.setId(id(answer.player(), answer.round(), answer.piece()));
        dto.setArtist(answer.artist());
        dto.setTitle(answer.title());
        dto.setPoints(answer.points());
        dto.setPlayer(answer.player());
        dto.setRound(answer.round());
        dto.setPiece(answer.piece());
        dto.setActualArtist(answer.actualArtist());
        dto.setActualTitle(answer.actualTitle());
        return dto;
    }

    private String id(String player, int round, int piece) {
        return round + ":" + piece + ":" + player;
    }

}
