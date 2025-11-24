package com.fraunhofer.sognoviz.entity;

import com.fraunhofer.sognoviz.util.DiagramMetadataConverter;
import com.powsybl.nad.svg.metadata.DiagramMetadata;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
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
    @Column(nullable = false)
    private String svgData;

    @Lob
    @Column(columnDefinition = "CLOB")
    @Convert(converter = DiagramMetadataConverter.class)
    private DiagramMetadata metadata;


    @Column(nullable = false)
    private String diagramType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Updated constructor
    public DiagramEntity(String name, String svgData, DiagramMetadata metadata, String diagramType) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.svgData = svgData;
        this.metadata = metadata;
        this.diagramType = diagramType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
