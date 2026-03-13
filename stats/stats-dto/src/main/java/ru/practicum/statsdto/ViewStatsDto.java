package ru.practicum.statsdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ViewStatsDto {

    @NotBlank
    private String app;

    @NotBlank
    private String uri;

    @NotNull
    private Long hits;

    public ViewStatsDto() {
    }

    public ViewStatsDto(String app, String uri, Long hits) {
        this.app = app;
        this.uri = uri;
        this.hits = hits;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Long getHits() {
        return hits;
    }

    public void setHits(Long hits) {
        this.hits = hits;
    }
}

