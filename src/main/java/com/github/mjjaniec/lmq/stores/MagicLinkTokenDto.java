package com.github.mjjaniec.lmq.stores;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

@Data
@Entity
@Table(name = "magic_link_token")
public class MagicLinkTokenDto {
    @Id
    private String token;

    private String email;
    private Instant expiresAt;
}
