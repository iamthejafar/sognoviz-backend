package com.fraunhofer.sognoviz.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.nad.svg.metadata.DiagramMetadata;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DiagramMetadataConverter implements AttributeConverter<DiagramMetadata, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(DiagramMetadata attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert DiagramMetadata to JSON", e);
        }
    }

    @Override
    public DiagramMetadata convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, DiagramMetadata.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to DiagramMetadata", e);
        }
    }
}
