package com.fraunhofer.sognoviz.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.nad.svg.metadata.DiagramMetadata;
import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Value
@Builder
public class DiagramFiles {
    String svgContent;
    DiagramMetadata jsonContent;
    String svgFileName;
    String jsonFileName;

    // Convert to bytes for ZIP
    public byte[] getSvgContentBytes() {
        return svgContent.getBytes(StandardCharsets.UTF_8);
    }

//    public byte[] getJsonContentBytes() {
//        return jsonContent.getBytes(StandardCharsets.UTF_8);
//    }

    // Create from SVG and JSON paths
    public static DiagramFiles fromPaths(Path svgPath, Path jsonPath) throws IOException {
        String svgContent = Files.readString(svgPath, StandardCharsets.UTF_8);
        String jsonContent = Files.readString(jsonPath, StandardCharsets.UTF_8);

        ObjectMapper objectMapper =new ObjectMapper();

        DiagramMetadata metadata = objectMapper.convertValue(jsonContent, DiagramMetadata.class);

        // Optional: validate JSON
        validateJson(jsonContent);

        return DiagramFiles.builder()
                .svgContent(svgContent)
                .jsonContent(metadata)
                .svgFileName(svgPath.getFileName().toString())
                .jsonFileName(jsonPath.getFileName().toString())
                .build();
    }

    // Optional JSON validation method
    private static void validateJson(String jsonContent) throws IOException {
        try {
            new ObjectMapper().readTree(jsonContent);
        } catch (Exception e) {
            throw new IOException("Invalid JSON content in DiagramFiles", e);
        }
    }

    // Factory for modified files (optional)
    public static DiagramFiles fromModifiedPaths(Path svgPath, Path jsonPath) throws IOException {
        return fromPaths(svgPath, jsonPath);
    }
}
