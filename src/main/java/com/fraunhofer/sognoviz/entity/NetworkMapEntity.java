package com.fraunhofer.sognoviz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "map_diagrams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkMapEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String svg;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String metadata;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String line;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String linePosition;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String substation;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String substationPosition;

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

    // Constructor without ID (for creating new entities)
    public NetworkMapEntity(String name, String svg, String metadata, String line,
                            String linePosition, String substation, String substationPosition,
                            String diagramType) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.svg = svg;
        this.metadata = metadata;
        this.line = line;
        this.linePosition = linePosition;
        this.substation = substation;
        this.substationPosition = substationPosition;
        this.diagramType = diagramType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}