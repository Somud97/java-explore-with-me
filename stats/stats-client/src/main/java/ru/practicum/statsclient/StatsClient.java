package ru.practicum.statsclient;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class StatsClient {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public StatsClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void hit(EndpointHitDto dto) {
        HttpEntity<EndpointHitDto> request = new HttpEntity<>(dto);
        restTemplate.postForEntity(baseUrl + "/hit", request, Void.class);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("start", start.format(formatter));
        params.add("end", end.format(formatter));
        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                params.add("uris", uri);
            }
        }
        params.add("unique", String.valueOf(unique));
        try {
            String url = baseUrl + "/stats" + buildQuery(params);
            URI uri = new URI(url);
            RequestEntity<Void> request = RequestEntity
                    .method(HttpMethod.GET, uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .build();
            ResponseEntity<ViewStatsDto[]> response =
                    restTemplate.exchange(url, HttpMethod.GET, request, ViewStatsDto[].class);
            ViewStatsDto[] body = response.getBody();
            if (body == null) {
                return List.of();
            }
            return Arrays.asList(body);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid stats service URL", e);
        }
    }

    private String buildQuery(MultiValueMap<String, String> params) {
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (String key : params.keySet()) {
            for (String value : params.get(key)) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(key).append("=").append(value);
                first = false;
            }
        }
        return sb.toString();
    }
}

