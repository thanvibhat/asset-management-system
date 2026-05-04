package com.assetmgmt.repository;

import com.assetmgmt.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByEntityTypeAndEntityId(Document.EntityType entityType, Long entityId);
    void deleteByEntityTypeAndEntityId(Document.EntityType entityType, Long entityId);
}
