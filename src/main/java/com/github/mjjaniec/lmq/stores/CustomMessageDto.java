package com.github.mjjaniec.lmq.stores;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "custom_message")
public class CustomMessageDto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String message;
}
