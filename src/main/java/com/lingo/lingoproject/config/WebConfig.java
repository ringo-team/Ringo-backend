package com.lingo.lingoproject.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

  @Value("${self-auth.url}")
  private String apiUrl;

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(15))
        .build();
  }

  @Bean
  public WebClient selfAuthWebClient(WebClient.Builder builder) {
    return builder
        .baseUrl(apiUrl)
        .build();
  }

  @Bean
  public ObjectMapper objectMapper(){
    return new ObjectMapper();
  }

}
