package com.assetmgmt.controller;

import com.assetmgmt.entity.Document;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.DocumentRepository;
import com.assetmgmt.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Document> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            Authentication authentication) throws IOException {
        Document doc = documentService.uploadDocument(
                file, Document.EntityType.valueOf(entityType), entityId, authentication.getName());
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Document>> getDocuments(
            @PathVariable String entityType, @PathVariable Long entityId) {
        return ResponseEntity.ok(documentService.getDocuments(Document.EntityType.valueOf(entityType), entityId));
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        Resource resource = documentService.downloadDocument(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getOriginalName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, doc.getContentType())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
