package com.github.mjjaniec.lmq.api;

import java.util.List;
import com.github.mjjaniec.lmq.model.SpreadsheetLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
public class HintController {

    public HintController(SpreadsheetLoader loader) {
        artists = loader.loadArtists();
        titles = loader.loadTitles();
    }

    private final List<String> artists;
    private final List<String> titles;

    @GetMapping("api/v1/hint/artist")
    public List<String> artist() {
       return artists;
    }

    @GetMapping("api/v1/hint/title")
    public List<String> title() {
        return titles;
    }
}
