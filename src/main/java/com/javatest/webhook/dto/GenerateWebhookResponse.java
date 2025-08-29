package com.javatest.webhook.dto;

import lombok.Data;

@Data
public class GenerateWebhookResponse {
    private String webhook;      // URL to submit the answer
    private String accessToken;  // JWT token to be used in Authorization header
}


