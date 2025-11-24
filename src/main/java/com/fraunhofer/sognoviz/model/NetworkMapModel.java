package com.fraunhofer.sognoviz.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkMapModel {
    private String id;
    private String name;
    private String svg;
    private String metadata;
    private String line;
    private String linePosition;
    private String substation;
    private String substationPosition;
    private String diagramType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}