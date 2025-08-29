package com.javatest.webhook.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String baseUrl;    // https://bfhldevapigw.healthrx.co.in/hiring
    private String track;      // JAVA
    private Candidate candidate = new Candidate();
    private Sql sql = new Sql();
    private Storage storage = new Storage();

    @Data
    public static class Candidate {
        private String name;
        private String regNo;
        private String email;
    }

    @Data
    public static class Sql {
        private String q1;
        private String q2;
    }

    @Data
    public static class Storage {
        private String outputDir;
    }
}

