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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                                         @RequestBody Map<String, Object> payload) {
        try {
            // 선택 범위를 먼저 비우고 덮어쓰기
            try { sheetsService.clearValues(spreadsheetId, range); } catch (Exception ignore) {}

            // 💡 Map에서 "values" 키로 데이터를 바로 가져옴 (타입 캐스팅)
            List<List<Object>> values = (List<List<Object>>) payload.get("values");

            // values가 null일 경우를 대비해 빈 리스트로 초기화
            if (values == null) {
                values = new ArrayList<>();
            }

            List<List<String>> stringValues = new ArrayList<>();
            for (List<Object> row : values) {
                List<String> newRow = new ArrayList<>();
                for (Object cell : row) {
                    // 각 셀의 Object를 String으로 변환 (null일 경우 빈 문자열로 처리)
                    newRow.add(cell != null ? cell.toString() : "");
                }
                stringValues.add(newRow);
            }

            String result = sheetsService.updateValues(spreadsheetId, range, valueInputOption, stringValues);
            return ResponseEntity.ok(result);

        } catch (HttpResponseException hre) {
            return ResponseEntity.status(hre.getStatusCode()).body(hre.getContent());
        } catch (Exception e) { // IOException, ClassCastException 등 처리
            return ResponseEntity.status(500).body("{\"message\": \"Error processing request: " + e.getMessage() + "\"}");
        }
    }

    // 💡 Jackson을 사용하도록 수정한 append 메소드
    @PostMapping("/applicants/append")
    public ResponseEntity<String> append(@RequestParam String spreadsheetId,
                                         @RequestParam(defaultValue = "A1:Z999") String range,
                                         @RequestParam(defaultValue = "USER_ENTERED") String valueInputOption,
                                         @RequestBody Map<String, Object> payload) { // 💡 JsonObject를 Map으로 변경
        try {
            // 💡 Map에서 "values" 키로 데이터를 바로 가져옴 (타입 캐스팅)
            List<List<Object>> values = (List<List<Object>>) payload.get("values");

            if (values == null) {
                values = new ArrayList<>();
            }

            List<List<String>> stringValues = new ArrayList<>();
            for (List<Object> row : values) {
                List<String> newRow = new ArrayList<>();
                for (Object cell : row) {
                    // 각 셀의 Object를 String으로 변환 (null일 경우 빈 문자열로 처리)
                    newRow.add(cell != null ? cell.toString() : "");
                }
                stringValues.add(newRow);
            }

            String result = sheetsService.appendValues(spreadsheetId, range, valueInputOption, stringValues);
            return ResponseEntity.ok(result);

        } catch (HttpResponseException hre) {
            return ResponseEntity.status(hre.getStatusCode()).body(hre.getContent());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\": \"Error processing request: " + e.getMessage() + "\"}");
        }
    }
}


