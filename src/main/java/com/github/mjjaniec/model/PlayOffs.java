package com.github.mjjaniec.model;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.helger.commons.io.resource.ClassPathResource;

import java.io.IOException;
import java.util.List;

public record PlayOffs(List<PlayOff> playOffs) {
    public record PlayOff(String name, int id, int value) {
    }

    public static final PlayOffs ThePlayOffs;

    static {
        ClassPathResource resource = new ClassPathResource("play-offs.yml");
        YAMLMapper yamlMapper = YAMLMapper.builder().build();
        try {
            ThePlayOffs = yamlMapper.reader().readValue(resource.getInputStream(), PlayOffs.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
