package com.project.hsf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "product_id")
    private SeafoodProduct product;

    @Size(max = 200)
    @NotNull
    @Nationalized
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotNull
    @Nationalized
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Size(max = 100)
    @Nationalized
    @Column(name = "category", length = 100)
    private String category;

    @Size(max = 500)
    @Nationalized
    @Column(name = "keywords", length = 500)
    private String keywords;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = false;

    @NotNull
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate = Instant.now();

    @NotNull
    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate = Instant.now();
}
