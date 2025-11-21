package com.fraunhofer.sognoviz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diagrams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String svgData;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String metadata;

    @Column(nullable = false)
    private String diagramType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString(); // ✅ Auto-generate UUID
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructor without ID (for creating new entities)
    public DiagramEntity(String name, String svgData, String metadata, String diagramType) {
        this.id = UUID.randomUUID().toString(); // ✅ ensure ID always present
        this.name = name;
        this.svgData = svgData;
        this.metadata = metadata;
        this.diagramType = diagramType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
