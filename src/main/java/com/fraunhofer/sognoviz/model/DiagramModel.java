package com.fraunhofer.sognoviz.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DiagramModel {
    private String id;
    private String name;
    private String svgData;
    @JsonRawValue
    private String metadata;
    private String diagramType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor without timestamps (for creating new diagrams)
    public DiagramModel(String name, String svgData, String metadata, String diagramType) {
        this.name = name;
        this.svgData = svgData;
        this.metadata = metadata;
        this.diagramType = diagramType;
    }


}