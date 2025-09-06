package com.edgekonkuk.edge_management.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GoogleSheetsService {

    private static final String SHEETS_VALUES_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s";
    private static final String SHEETS_APPEND_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s:append?valueInputOption=%s";
    private static final String SHEETS_SPREADSHEET_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s?fields=sheets(properties(title,sheetId,gridProperties(rowCount,columnCount)))";
    private static final String SHEETS_CLEAR_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s:clear";

    private final GoogleCredentials credentials;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    public GoogleSheetsService(GoogleCredentials credentials,
                               HttpTransport httpTransport,
                               JsonFactory jsonFactory) {
        this.credentials = credentials;
        this.httpTransport = httpTransport;
        this.jsonFactory = jsonFactory;
    }

    public String getValues(String spreadsheetId, String rangeA1) throws IOException {
        HttpRequestFactory factory = httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));
        String encRange = java.net.URLEncoder.encode(rangeA1, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format(SHEETS_VALUES_URL, spreadsheetId, encRange);
        HttpRequest request = factory.buildGetRequest(new GenericUrl(url));
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String updateValues(String spreadsheetId, String rangeA1, String valueInputOption,
                               java.util.List<java.util.List<String>> values) throws IOException {
        if (rangeA1 == null || rangeA1.isBlank()) {
            rangeA1 = "A1:Z999";
        }
        if (valueInputOption == null || valueInputOption.isBlank()) {
            valueInputOption = "USER_ENTERED";
        }
        HttpRequestFactory factory = httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));
        String encRange = java.net.URLEncoder.encode(rangeA1, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format(SHEETS_VALUES_URL, spreadsheetId, encRange)
                + "?valueInputOption=" + java.net.URLEncoder.encode(valueInputOption, java.nio.charset.StandardCharsets.UTF_8)
                + "&includeValuesInResponse=true"
                + "&responseValueRenderOption=UNFORMATTED_VALUE";

        var body = new com.google.gson.JsonObject();
        body.addProperty("majorDimension", "ROWS");
        var arr = new com.google.gson.JsonArray();
        for (var row : values) {
            var jr = new com.google.gson.JsonArray();
            for (var v : row) { jr.add(v); }
            arr.add(jr);
        }
        body.add("values", arr);

        var content = new com.google.api.client.http.ByteArrayContent("application/json",
                body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest request = factory.buildPutRequest(new GenericUrl(url), content);
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String appendValues(String spreadsheetId, String rangeA1, String valueInputOption,
                               java.util.List<java.util.List<String>> values) throws IOException {
        if (rangeA1 == null || rangeA1.isBlank()) {
            rangeA1 = "A1:Z999";
        }
        if (valueInputOption == null || valueInputOption.isBlank()) {
            valueInputOption = "USER_ENTERED";
        }
        HttpRequestFactory factory = httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));
        String url = String.format(SHEETS_APPEND_URL, spreadsheetId,
                java.net.URLEncoder.encode(rangeA1, java.nio.charset.StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(valueInputOption, java.nio.charset.StandardCharsets.UTF_8));

        var body = new com.google.gson.JsonObject();
        body.addProperty("majorDimension", "ROWS");
        var arr = new com.google.gson.JsonArray();
        for (var row : values) {
            var jr = new com.google.gson.JsonArray();
            for (var v : row) { jr.add(v); }
            arr.add(jr);
        }
        body.add("values", arr);

        var content = new com.google.api.client.http.ByteArrayContent("application/json",
                body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest request = factory.buildPostRequest(new GenericUrl(url), content);
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String clearValues(String spreadsheetId, String rangeA1) throws IOException {
        HttpRequestFactory factory = httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));
        String encRange = java.net.URLEncoder.encode(rangeA1, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format(SHEETS_CLEAR_URL, spreadsheetId, encRange);
        var content = new com.google.api.client.http.ByteArrayContent("application/json",
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest request = factory.buildPostRequest(new GenericUrl(url), content);
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String getSpreadsheetMetadata(String spreadsheetId) throws IOException {
        HttpRequestFactory factory = httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));
        String url = String.format(SHEETS_SPREADSHEET_URL, spreadsheetId);
        HttpRequest request = factory.buildGetRequest(new GenericUrl(url));
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }
}


