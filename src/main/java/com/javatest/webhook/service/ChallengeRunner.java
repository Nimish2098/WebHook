package com.javatest.webhook.service;

import com.javatest.webhook.config.AppProperties;
import com.javatest.webhook.dto.GenerateWebhookRequest;
import com.javatest.webhook.dto.GenerateWebhookResponse;
import com.javatest.webhook.util.RegNoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeRunner implements ApplicationRunner {

    private final AppProperties props;
    private final WebhookClient client;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting Webhook Challenge Runner...");

        // 1) Generate webhook & token
        GenerateWebhookRequest req = new GenerateWebhookRequest(
                props.getCandidate().getName(),
                props.getCandidate().getRegNo(),
                props.getCandidate().getEmail()
        );

        GenerateWebhookResponse res = client
                .generateWebhook(props.getBaseUrl(), props.getTrack(), req)
                .block();

        if (res == null || res.getWebhook() == null || res.getAccessToken() == null) {
            throw new IllegalStateException("Invalid response from generateWebhook API");
        }

        // 2) Decide which SQL to use based on regNo parity
        boolean odd = RegNoUtil.isOddByLastTwoDigits(props.getCandidate().getRegNo());
        String chosenSql = odd ? props.getSql().getQ1() : props.getSql().getQ2();
        String question = odd ? "Q1 (odd)" : "Q2 (even)";
        log.info("regNo parity: {} → using {}", odd ? "odd" : "even", question);

        // 3) Store the final query locally
        persistSolution(chosenSql, question);

        // 4) Submit to dynamic webhook
        client.submitToWebhook(res.getWebhook(), res.getAccessToken(), chosenSql)
                .doOnSuccess(v -> log.info("Webhook submission OK"))
                .doOnError(e -> log.warn("Webhook submission failed: {}", e.toString()))
                .onErrorResume(e -> {
                    // 5) Try fallback endpoint if webhook submission fails
                    return client.submitToFallback(props.getBaseUrl(), props.getTrack(), res.getAccessToken(), chosenSql)
                            .doOnSuccess(v2 -> log.info("Fallback submission OK"));
                })
                .block();

        log.info("Challenge run complete.");
    }

    private void persistSolution(String sql, String question) throws IOException {
        Path dir = Path.of(props.getStorage().getOutputDir());
        Files.createDirectories(dir);
        String json = toJson(Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "question", question,
                "regNo", props.getCandidate().getRegNo(),
                "finalQuery", sql
        ));
        Path out = dir.resolve("solution.json");
        Files.writeString(out, json);
        log.info("Saved solution to {}", out.toAbsolutePath());
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('\"').append(escape(e.getKey())).append('\"').append(":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else sb.append('\"').append(escape(v.toString())).append('\"');
        }
        sb.append('}');
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}

