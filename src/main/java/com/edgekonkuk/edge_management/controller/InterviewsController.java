package com.edgekonkuk.edge_management.controller;

import com.edgekonkuk.edge_management.service.GoogleSheetsService;
import com.google.api.client.http.HttpResponseException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/interviews")
public class InterviewsController {

    private final GoogleSheetsService sheetsService;

    public InterviewsController(GoogleSheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    // 스프레드시트 메타데이터(시트 탭 이름/크기 등)
    @GetMapping("/applicants/meta")
    public ResponseEntity<String> meta(@RequestParam String spreadsheetId) throws IOException {
        try {
            return ResponseEntity.ok(sheetsService.getSpreadsheetMetadata(spreadsheetId));
        } catch (HttpResponseException hre) {
            int status = hre.getStatusCode();
            String body = hre.getContent();
            if (body == null || body.isBlank()) body = "Google API error";
            return ResponseEntity.status(status).body(body);
        }
    }

    // 시트에서 지원자 데이터 조회
    @GetMapping("/applicants")
    public ResponseEntity<String> applicants(@RequestParam String spreadsheetId,
                                             @RequestParam(defaultValue = "A1:Z999") String range) throws IOException {
        try {
            return ResponseEntity.ok(sheetsService.getValues(spreadsheetId, range));
        } catch (HttpResponseException hre) {
            int status = hre.getStatusCode();
            String body = hre.getContent();
            if (body == null || body.isBlank()) body = "Google API error";
            return ResponseEntity.status(status).body(body);
        }
    }

    // 시트 값 덮어쓰기 업데이트
    @PostMapping("/applicants/update")
    public ResponseEntity<String> update(@RequestParam String spreadsheetId,
                                         @RequestParam(defaultValue = "A1:Z999") String range,
                                         @RequestParam(defaultValue = "USER_ENTERED") String valueInputOption,
                                         @RequestBody JsonObject payload) throws IOException {
        try {
            // 선택 범위를 먼저 비우고 덮어쓰기(응답의 updatedRange가 A1로만 나오는 케이스 방지)
            try { sheetsService.clearValues(spreadsheetId, range); } catch (Exception ignore) {}
            JsonArray rows = payload.getAsJsonArray("values");
            java.util.List<java.util.List<String>> values = new java.util.ArrayList<>();
            if (rows != null) {
                for (JsonElement e : rows) {
                    JsonArray r = e.getAsJsonArray();
                    java.util.List<String> row = new java.util.ArrayList<>();
                    for (JsonElement c : r) row.add(c.isJsonNull()?"":c.getAsString());
                    values.add(row);
                }
            }
            return ResponseEntity.ok(sheetsService.updateValues(spreadsheetId, range, valueInputOption, values));
        } catch (HttpResponseException hre) {
            int status = hre.getStatusCode();
            String body = hre.getContent();
            if (body == null || body.isBlank()) body = "Google API error";
            return ResponseEntity.status(status).body(body);
        }
    }

    // 시트 끝에 추가
    @PostMapping("/applicants/append")
    public ResponseEntity<String> append(@RequestParam String spreadsheetId,
                                         @RequestParam(defaultValue = "A1:Z999") String range,
                                         @RequestParam(defaultValue = "USER_ENTERED") String valueInputOption,
                                         @RequestBody JsonObject payload) throws IOException {
        try {
            JsonArray rows = payload.getAsJsonArray("values");
            java.util.List<java.util.List<String>> values = new java.util.ArrayList<>();
            if (rows != null) {
                for (JsonElement e : rows) {
                    JsonArray r = e.getAsJsonArray();
                    java.util.List<String> row = new java.util.ArrayList<>();
                    for (JsonElement c : r) row.add(c.isJsonNull()?"":c.getAsString());
                    values.add(row);
                }
            }
            return ResponseEntity.ok(sheetsService.appendValues(spreadsheetId, range, valueInputOption, values));
        } catch (HttpResponseException hre) {
            int status = hre.getStatusCode();
            String body = hre.getContent();
            if (body == null || body.isBlank()) body = "Google API error";
            return ResponseEntity.status(status).body(body);
        }
    }
}


