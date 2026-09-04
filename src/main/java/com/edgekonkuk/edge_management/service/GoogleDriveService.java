package com.edgekonkuk.edge_management.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleDriveService {

    private static final String DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private static final String DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

    private final GoogleCredentials credentials;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    @Value("${google.drive.driveId:}")
    private String driveId;

    public GoogleDriveService(GoogleCredentials credentials,
                              HttpTransport httpTransport,
                              JsonFactory jsonFactory) {
        this.credentials = credentials;
        this.httpTransport = httpTransport;
        this.jsonFactory = jsonFactory;
    }

    private HttpRequestFactory factory() {
        return httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));
    }

    public String listFiles() throws IOException {
        HttpRequestFactory factory = factory();
        String base = DRIVE_FILES_URL + "?pageSize=10&fields=files(id,name,mimeType)" +
                "&supportsAllDrives=true&includeItemsFromAllDrives=true" +
                (driveId == null || driveId.isBlank() ? "&corpora=allDrives" : "&corpora=drive&driveId=" + java.net.URLEncoder.encode(driveId, java.nio.charset.StandardCharsets.UTF_8));
        GenericUrl url = new GenericUrl(base);
        HttpRequest request = factory.buildGetRequest(url);
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String getFile(String fileId) throws IOException {
        HttpRequestFactory factory = factory();
        String url = DRIVE_FILES_URL + "/" + java.net.URLEncoder.encode(fileId, java.nio.charset.StandardCharsets.UTF_8)
                + "?fields=id,name,mimeType,shortcutDetails(targetId,targetMimeType)";
        HttpRequest request = factory.buildGetRequest(new GenericUrl(url));
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    private String listByQuery(String q, String fields) throws IOException {
        HttpRequestFactory factory = factory();
        StringBuilder sb = new StringBuilder(DRIVE_FILES_URL)
                .append("?q=").append(java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8))
                .append("&fields=").append(java.net.URLEncoder.encode(fields, java.nio.charset.StandardCharsets.UTF_8))
                .append("&pageSize=1000")
                .append("&supportsAllDrives=true&includeItemsFromAllDrives=true");
        if (driveId == null || driveId.isBlank()) {
            sb.append("&corpora=allDrives");
        } else {
            sb.append("&corpora=drive&driveId=")
              .append(java.net.URLEncoder.encode(driveId, java.nio.charset.StandardCharsets.UTF_8));
        }
        String url = sb.toString();
        HttpRequest request = factory.buildGetRequest(new GenericUrl(url));
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String getRootParentId() {
        return (driveId == null || driveId.isBlank()) ? "root" : driveId;
    }

    public String findFolderByName(String name) throws IOException {
        String q = String.format("mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false", name.replace("'", "\\'"));
        return listByQuery(q, "files(id,name,mimeType,parents)");
    }

    public String listChildren(String parentId) throws IOException {
        String q;
        if (parentId == null || parentId.isBlank() || "root".equals(parentId)) {
            // Top-level: show items in My Drive root OR shared with this service account
            q = "( 'root' in parents or sharedWithMe = true ) and trashed = false";
        } else {
            q = String.format("'%s' in parents and trashed=false", parentId);
        }
        // include shortcutDetails to resolve shortcuts to their targets
        return listByQuery(q, "files(id,name,mimeType,parents,shortcutDetails(targetId,targetMimeType))");
    }

    public String searchResponseSheetsUnderRoot(String rootFolderName) throws IOException {
        String rootJson = findFolderByName(rootFolderName);
        var parser = new com.google.gson.JsonParser();
        var rootObj = parser.parse(rootJson).getAsJsonObject();
        var filesArr = rootObj.getAsJsonArray("files");
        if (filesArr == null || filesArr.size() == 0) return "{\"items\":[]}";
        String rootId = filesArr.get(0).getAsJsonObject().get("id").getAsString();

        String yearQ = String.format("'%s' in parents and trashed=false and mimeType='application/vnd.google-apps.folder'", rootId);
        String yearJson = listByQuery(yearQ, "files(id,name,parents)");
        var yearObj = parser.parse(yearJson).getAsJsonObject();
        var yearArr = yearObj.getAsJsonArray("files");

        var result = new com.google.gson.JsonArray();
        if (yearArr != null) {
            for (var el : yearArr) {
                var y = el.getAsJsonObject();
                String yName = y.get("name").getAsString();
                if (!yName.startsWith("운영활동_")) continue;
                String yId = y.get("id").getAsString();
                String recQ = String.format("'%s' in parents and trashed=false and mimeType='application/vnd.google-apps.folder'", yId);
                String recJson = listByQuery(recQ, "files(id,name,parents)");
                var recArr = parser.parse(recJson).getAsJsonObject().getAsJsonArray("files");
                if (recArr == null) continue;
                for (var rEl : recArr) {
                    var r = rEl.getAsJsonObject();
                    String rName = r.get("name").getAsString();
                    if (!rName.contains("신입부원 모집")) continue;
                    String rId = r.get("id").getAsString();
                    String filesQ = String.format("'%s' in parents and trashed=false and mimeType='application/vnd.google-apps.spreadsheet' and name contains '응답'", rId);
                    String filesJson = listByQuery(filesQ, "files(id,name,parents)");
                    var fArr = parser.parse(filesJson).getAsJsonObject().getAsJsonArray("files");
                    if (fArr == null) continue;
                    for (var fEl : fArr) {
                        var f = fEl.getAsJsonObject();
                        var item = new com.google.gson.JsonObject();
                        item.addProperty("id", f.get("id").getAsString());
                        item.addProperty("name", f.get("name").getAsString());
                        item.addProperty("path", rootFolderName + "/" + yName + "/" + rName + "/" + f.get("name").getAsString());
                        result.add(item);
                    }
                }
            }
        }
        var out = new com.google.gson.JsonObject();
        out.add("items", result);
        return out.toString();
    }

    public String createFolder(String name, String parentId) throws IOException {
        HttpRequestFactory factory = factory();
        String metadataJson = parentId == null || parentId.isBlank()
                ? String.format("{\"name\":\"%s\",\"mimeType\":\"application/vnd.google-apps.folder\"}", name)
                : String.format("{\"name\":\"%s\",\"mimeType\":\"application/vnd.google-apps.folder\",\"parents\":[\"%s\"]}", name, parentId);

        MultipartContent content = new MultipartContent().setMediaType(new HttpMediaType("multipart/related").setParameter("boundary", "-edge-boundary"));
        content.addPart(new MultipartContent.Part(
                new com.google.api.client.http.HttpHeaders().set("Content-Type", "application/json; charset=UTF-8"),
                new ByteArrayContent("application/json", metadataJson.getBytes(StandardCharsets.UTF_8))
        ));

        HttpRequest request = factory.buildPostRequest(new GenericUrl(DRIVE_UPLOAD_URL), content);
        request.getHeaders().set("Content-Type", "multipart/related; boundary=-edge-boundary");
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String uploadFile(String name, byte[] data, String mimeType, String parentId) throws IOException {
        HttpRequestFactory factory = factory();
        String metadataJson = parentId == null || parentId.isBlank()
                ? String.format("{\"name\":\"%s\"}", name)
                : String.format("{\"name\":\"%s\",\"parents\":[\"%s\"]}", name, parentId);

        MultipartContent content = new MultipartContent().setMediaType(new HttpMediaType("multipart/related").setParameter("boundary", "-edge-boundary"));
        content.addPart(new MultipartContent.Part(
                new com.google.api.client.http.HttpHeaders().set("Content-Type", "application/json; charset=UTF-8"),
                new ByteArrayContent("application/json", metadataJson.getBytes(StandardCharsets.UTF_8))
        ));
        content.addPart(new MultipartContent.Part(
                new com.google.api.client.http.HttpHeaders().set("Content-Type", mimeType),
                new ByteArrayContent(mimeType, data)
        ));

        HttpRequest request = factory.buildPostRequest(new GenericUrl(DRIVE_UPLOAD_URL), content);
        request.getHeaders().set("Content-Type", "multipart/related; boundary=-edge-boundary");
        request.setParser(new JsonObjectParser(jsonFactory));
        HttpResponse response = request.execute();
        return response.parseAsString();
    }

    public String deleteFile(String fileId) throws IOException {
        HttpRequestFactory factory = factory();
        HttpRequest request = factory.buildDeleteRequest(new GenericUrl(DRIVE_FILES_URL + "/" + fileId));
        HttpResponse response = request.execute();
        return String.valueOf(response.getStatusCode());
    }
}

