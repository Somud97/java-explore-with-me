package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsclient.StatsClient;

@Configuration
public class StatsClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public StatsClient statsClient(RestTemplate restTemplate,
                                   @Value("${stats-server.url:http://localhost:9090}") String baseUrl) {
        return new StatsClient(restTemplate, baseUrl);
    }
}
