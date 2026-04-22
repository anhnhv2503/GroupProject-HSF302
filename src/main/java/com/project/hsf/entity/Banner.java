package com.project.hsf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "banners")
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 200)
    @Nationalized
    @Column(name = "title", length = 200)
    private String title;

    @Size(max = 500)
    @NotNull
    @Nationalized
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Size(max = 500)
    @Nationalized
    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Size(max = 500)
    @Nationalized
    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "display_order")
    private Integer displayOrder;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @ColumnDefault("getdate()")
    @Column(name = "created_date")
    private Instant createdDate;

    @UpdateTimestamp
    @ColumnDefault("getdate()")
    @Column(name = "updated_date")
    private Instant updatedDate;
}
