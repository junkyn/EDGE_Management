package com.edgekonkuk.edge_management.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class GoogleAuthConfig {

    @Value("${google.service.account.keyPath}")
    private String serviceAccountKeyPath;

    @Value("${google.oauth.scopes:}")
    private String oauthScopesRaw;

    @Value("${google.drive.scopes:}")
    private String driveScopesRaw;

    @Value("${google.sheets.scopes:}")
    private String sheetsScopesRaw;

    @Bean
    public JsonFactory jsonFactory() {
        return JacksonFactory.getDefaultInstance();
    }

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        List<String> scopes = new ArrayList<>();
        if (oauthScopesRaw != null && !oauthScopesRaw.isBlank()) {
            scopes.addAll(Arrays.stream(oauthScopesRaw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
        }
        if (driveScopesRaw != null && !driveScopesRaw.isBlank()) {
            scopes.addAll(Arrays.stream(driveScopesRaw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
        }
        if (sheetsScopesRaw != null && !sheetsScopesRaw.isBlank()) {
            scopes.addAll(Arrays.stream(sheetsScopesRaw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
        }
        // Resolve service account key InputStream
        InputStream keyStream = null;
        String resolved = serviceAccountKeyPath;
        if (resolved == null || resolved.isBlank()) {
            resolved = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        }
        if (resolved != null && !resolved.isBlank()) {
            if (resolved.startsWith("classpath:")) {
                String cp = resolved.substring("classpath:".length());
                ClassPathResource cpr = new ClassPathResource(cp.startsWith("/") ? cp.substring(1) : cp);
                if (cpr.exists()) {
                    keyStream = cpr.getInputStream();
                }
            } else {
                Path p = Paths.get(resolved);
                if (!p.isAbsolute()) {
                    p = Paths.get("").toAbsolutePath().resolve(resolved).normalize();
                }
                if (Files.exists(p)) {
                    keyStream = Files.newInputStream(p);
                }
            }
        }
        if (keyStream == null) {
            // Try default classpath location
            ClassPathResource cpr = new ClassPathResource("credentials/service-account.json");
            if (cpr.exists()) {
                keyStream = cpr.getInputStream();
            } else {
                throw new IOException("Service account key not found. Set google.service.account.keyPath or GOOGLE_APPLICATION_CREDENTIALS, or place credentials/service-account.json on classpath.");
            }
        }

        GoogleCredentials base;
        try (InputStream in = keyStream) {
            base = GoogleCredentials.fromStream(in);
        }
        GoogleCredentials scoped = scopes.isEmpty() ? base : base.createScoped(scopes);
        return scoped;
    }
}

