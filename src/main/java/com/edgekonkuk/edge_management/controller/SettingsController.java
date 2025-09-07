package com.edgekonkuk.edge_management.controller;

import com.edgekonkuk.edge_management.service.GoogleSheetsService;
import com.google.api.client.http.HttpResponseException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingsController {

    // 서버 코드에서만 변경 가능한 세팅 파일 ID (스프레드시트)
    private static final String SETTINGS_SPREADSHEET_ID = "1iE-OuKqkOOhAZY6RvN1WZ5nvHnVXhgCqcs6BILY-AJM"; // TODO: 실제 ID로 교체

    // 시트 탭/범위 상수
    private static final String CONFIG_SHEET_RANGE = "CONFIG!A1:B99"; // key,value 테이블
    private static final String INTERVIEW_SETTINGS_RANGE = "INTERVIEW_SETTINGS!1:2"; // 1행: 질문, 2행: 점수영역

    private final GoogleSheetsService sheetsService;
    private final Gson gson = new Gson();

    public SettingsController(GoogleSheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    @GetMapping("/settings")
    public ResponseEntity<String> getSettings() {
        try {
            // CONFIG 시트에서 key-value 읽기
            String configRaw = sheetsService.getValues(SETTINGS_SPREADSHEET_ID, CONFIG_SHEET_RANGE);
            JsonObject configObj = gson.fromJson(configRaw, JsonObject.class);
            List<List<String>> configRows = extractRows(configObj);
            Map<String, String> kv = new HashMap<>();
            for (List<String> row : configRows) {
                if (row.size() >= 2) {
                    String key = row.get(0) == null ? "" : row.get(0).trim();
                    String val = row.get(1) == null ? "" : row.get(1).trim();
                    if (!key.isEmpty()) kv.put(key, val);
                }
            }

            // INTERVIEW_SETTINGS 시트에서 질문/점수영역 읽기
            String isRaw = sheetsService.getValues(SETTINGS_SPREADSHEET_ID, INTERVIEW_SETTINGS_RANGE);
            JsonObject isObj = gson.fromJson(isRaw, JsonObject.class);
            List<List<String>> isRows = extractRows(isObj);
            List<String> questions = isRows.size() >= 1 ? isRows.get(0) : new ArrayList<>();
            List<String> scoreAreas = isRows.size() >= 2 ? isRows.get(1) : new ArrayList<>();

            JsonObject out = new JsonObject();
            out.addProperty("applicantsFileId", kv.getOrDefault("APPLICANTS_FILE_ID", ""));
            out.addProperty("evaluationFileId", kv.getOrDefault("EVALUATION_FILE_ID", ""));

            JsonArray qArr = new JsonArray();
            for (String q : questions) if (q != null && !q.isBlank()) qArr.add(q);
            JsonArray aArr = new JsonArray();
            for (String a : scoreAreas) if (a != null && !a.isBlank()) aArr.add(a);
            out.add("questions", qArr);
            out.add("scoreAreas", aArr);

            return ResponseEntity.ok(out.toString());
        } catch (HttpResponseException hre) {
            return ResponseEntity.status(hre.getStatusCode()).body(hre.getContent());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\":\"Failed to load settings: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/settings/update")
    public ResponseEntity<String> updateSettings(@RequestBody Map<String, Object> body) {
        try {
            String applicantsFileId = toStr(body.get("applicantsFileId"));
            String evaluationFileId = toStr(body.get("evaluationFileId"));
            List<String> questions = toStrList(body.get("questions"));
            List<String> scoreAreas = toStrList(body.get("scoreAreas"));

            // CONFIG 업데이트 (key,value)
            List<List<String>> configValues = new ArrayList<>();
            configValues.add(List.of("APPLICANTS_FILE_ID", applicantsFileId == null ? "" : applicantsFileId));
            configValues.add(List.of("EVALUATION_FILE_ID", evaluationFileId == null ? "" : evaluationFileId));
            sheetsService.clearValues(SETTINGS_SPREADSHEET_ID, CONFIG_SHEET_RANGE);
            sheetsService.updateValues(SETTINGS_SPREADSHEET_ID, CONFIG_SHEET_RANGE, "USER_ENTERED", configValues);

            // INTERVIEW_SETTINGS 업데이트 (1행 질문, 2행 영역)
            List<List<String>> isValues = new ArrayList<>();
            isValues.add(questions == null ? List.of() : questions);
            isValues.add(scoreAreas == null ? List.of() : scoreAreas);
            sheetsService.clearValues(SETTINGS_SPREADSHEET_ID, INTERVIEW_SETTINGS_RANGE);
            sheetsService.updateValues(SETTINGS_SPREADSHEET_ID, INTERVIEW_SETTINGS_RANGE, "USER_ENTERED", isValues);

            return ResponseEntity.ok("{\"message\":\"Settings updated\"}");
        } catch (HttpResponseException hre) {
            return ResponseEntity.status(hre.getStatusCode()).body(hre.getContent());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"message\":\"Failed to update settings: " + e.getMessage() + "\"}");
        }
    }

    // --- helpers ---
    private static List<List<String>> extractRows(JsonObject valuesResponse) {
        List<List<String>> rows = new ArrayList<>();
        if (valuesResponse == null || !valuesResponse.has("values")) return rows;
        for (var el : valuesResponse.getAsJsonArray("values")) {
            List<String> row = new ArrayList<>();
            for (var cell : el.getAsJsonArray()) {
                row.add(cell.isJsonNull() ? "" : cell.getAsString());
            }
            rows.add(row);
        }
        return rows;
    }

    private static String toStr(Object o) { return o == null ? null : String.valueOf(o); }
    @SuppressWarnings("unchecked")
    private static List<String> toStrList(Object o) {
        if (o == null) return new ArrayList<>();
        if (o instanceof List<?>) {
            List<String> out = new ArrayList<>();
            for (Object v : (List<Object>) o) out.add(v == null ? "" : String.valueOf(v));
            return out;
        }
        return new ArrayList<>();
    }
}


