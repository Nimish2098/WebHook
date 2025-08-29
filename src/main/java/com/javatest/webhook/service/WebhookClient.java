package com.javatest.webhook.service;

import com.javatest.webhook.dto.GenerateWebhookRequest;
import com.javatest.webhook.dto.GenerateWebhookResponse;
import com.javatest.webhook.dto.FinalQueryPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookClient {

    private final WebClient webClient = WebClient.builder().build();

    public Mono<GenerateWebhookResponse> generateWebhook(String baseUrl, String track, GenerateWebhookRequest body) {
        String url = String.format("%s/generateWebhook/%s", baseUrl, track);
        log.info("POST {}", url);
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GenerateWebhookResponse.class)
                .doOnNext(res -> log.info("Received webhook URL and access token"));
    }

    public Mono<Void> submitToWebhook(String webhookUrl, String accessToken, String finalQuery) {
        log.info("Submitting finalQuery to webhook: {}", webhookUrl);
        return webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.set("Authorization", accessToken)) // spec says raw token
                .bodyValue(new FinalQueryPayload(finalQuery))
                .retrieve()
                .bodyToMono(Void.class)
                .then();
    }

    public Mono<Void> submitToFallback(String baseUrl, String track, String accessToken, String finalQuery) {
        String url = String.format("%s/testWebhook/%s", baseUrl, track);
        log.info("Submitting finalQuery to fallback: {}", url);
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.set("Authorization", accessToken))
                .bodyValue(new FinalQueryPayload(finalQuery))
                .retrieve()
                .bodyToMono(Void.class)
                .then();
    }
}


