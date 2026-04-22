package com.project.hsf.repository;

import com.project.hsf.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    @Query(value = "SELECT TOP 3 * FROM knowledge_documents " +
           "WHERE active = 1 AND (content LIKE %:keyword% OR title LIKE %:keyword% OR keywords LIKE %:keyword%)", 
           nativeQuery = true)
    List<KnowledgeDocument> searchByKeywords(@Param("keyword") String keyword);
}
