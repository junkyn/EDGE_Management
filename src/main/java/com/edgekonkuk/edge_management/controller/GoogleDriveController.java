package com.edgekonkuk.edge_management.controller;

import com.edgekonkuk.edge_management.service.GoogleDriveService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/drive")
public class GoogleDriveController {

    private final GoogleDriveService driveService;

    public GoogleDriveController(GoogleDriveService driveService) {
        this.driveService = driveService;
    }

    // 3) 파일 목록 조회
    @GetMapping("/files")
    public ResponseEntity<String> list() throws IOException {
        return ResponseEntity.ok(driveService.listFiles());
    }

    // 4) 폴더 생성
    @PostMapping("/folders")
    public ResponseEntity<String> createFolder(@RequestParam String name,
                                               @RequestParam(required = false) String parentId) throws IOException {
        return ResponseEntity.ok(driveService.createFolder(name, parentId));
    }

    // 5) 파일 업로드
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam String name,
                                         @RequestParam(required = false) String parentId,
                                         @RequestPart("file") MultipartFile file) throws IOException {
        byte[] data = file.getBytes();
        String mimeType = file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
        return ResponseEntity.ok(driveService.uploadFile(name, data, mimeType, parentId));
    }

    // 6) 파일 삭제
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<String> delete(@PathVariable String fileId) throws IOException {
        return ResponseEntity.ok(driveService.deleteFile(fileId));
    }

    // 7) 'EDGE' 하위 구조에서 응답 스프레드시트 검색
    @GetMapping("/search/response-sheets")
    public ResponseEntity<String> responseSheets(@RequestParam(defaultValue = "EDGE") String root) throws IOException {
        return ResponseEntity.ok(driveService.searchResponseSheetsUnderRoot(root));
    }

    // 8) 파일 탐색: children 조회
    @GetMapping("/browse")
    public ResponseEntity<String> browse(@RequestParam(required = false) String parentId) throws IOException {
        String pid = (parentId == null || parentId.isBlank()) ? driveService.getRootParentId() : parentId;
        return ResponseEntity.ok(driveService.listChildren(pid));
    }

    @GetMapping("/file")
    public ResponseEntity<String> file(@RequestParam String fileId) throws IOException {
        return ResponseEntity.ok(driveService.getFile(fileId));
    }
}


