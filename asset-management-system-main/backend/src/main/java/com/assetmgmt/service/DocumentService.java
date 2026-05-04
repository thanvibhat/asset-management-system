package com.assetmgmt.service;

import com.assetmgmt.entity.Document;
import com.assetmgmt.entity.User;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.DocumentRepository;
import com.assetmgmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public Document uploadDocument(MultipartFile file, Document.EntityType entityType, Long entityId, String uploaderUsername) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), dir.resolve(uniqueFileName), StandardCopyOption.REPLACE_EXISTING);

        User uploader = userRepository.findByUsername(uploaderUsername).orElse(null);

        Document document = Document.builder()
                .fileName(uniqueFileName)
                .originalName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .entityType(entityType)
                .entityId(entityId)
                .uploadedBy(uploader)
                .build();

        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocuments(Document.EntityType entityType, Long entityId) {
        return documentRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(document.getFileName()));
        } catch (IOException e) {
            // Log error or handle as needed
        }

        documentRepository.delete(document);
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(Long documentId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        Path filePath = Paths.get(uploadDir).resolve(document.getFileName());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Document file not found or not readable", documentId);
        }

        return resource;
    }
}
