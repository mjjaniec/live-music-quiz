package com.github.mjjaniec.lmq.stores;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "maestro")
public class MaestroDto {
    @Id
    private UUID id;

    @Column(unique = true)
    private String email;
}
