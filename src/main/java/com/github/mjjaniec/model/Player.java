package com.github.mjjaniec.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "players")
@RequiredArgsConstructor
public class Player {
    @Id
    public final String name;

    private Player() {
        name = "";
    }
}
