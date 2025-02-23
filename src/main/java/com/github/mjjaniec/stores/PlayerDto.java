package com.github.mjjaniec.stores;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "player")
public class PlayerDto {
    @Id
    private String name;
}
