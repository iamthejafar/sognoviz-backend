package com.fraunhofer.sognoviz.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraunhofer.sognoviz.model.DiagramFiles;
import com.fraunhofer.sognoviz.model.DiagramModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
public class DiagramFileHelper {

    public static final String CGMES_STORAGE_DIR = "./cgmes/";
    private static final String SVG_EXTENSION = ".svg";
    private static final String JSON_EXTENSION = ".json";
    private static final String ZIP_EXTENSION = ".zip";

    private final  DiagramMetadataConverter diagramMetadataConverter = new DiagramMetadataConverter();

    public static String updateFileName(String originalFileName, String newBaseName, String newExtension) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("Original filename cannot be null or empty");
        }

        String baseName = originalFileName;
        String extension = "";

        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex != -1) {
            baseName = originalFileName.substring(0, dotIndex);
            extension = originalFileName.substring(dotIndex + 1);
        }

        // If new base name is provided
        if (newBaseName != null && !newBaseName.isEmpty()) {
            baseName = newBaseName;
        }

        // If new extension is provided
        if (newExtension != null && !newExtension.isEmpty()) {
            extension = newExtension.startsWith(".") ? newExtension.substring(1) : newExtension;
        }

        return extension.isEmpty() ? baseName : baseName + "." + extension;
    }


    public Path storeUploadedFile(MultipartFile file, String id) throws IOException {
        Path storageDir = Paths.get(CGMES_STORAGE_DIR);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
        Path storedFile = storageDir.resolve(id + ZIP_EXTENSION);
        file.transferTo(storedFile);
        return storedFile;
    }

    public DiagramFiles readDiagramFiles(Path outputDir, String baseName) throws IOException {
        Path svgFile = outputDir.resolve(baseName + SVG_EXTENSION);
        Path jsonFile = outputDir.resolve(baseName + "_metadata" + JSON_EXTENSION);

        validateFileExists(svgFile, "SVG");
        validateFileExists(jsonFile, "JSON");

        return DiagramFiles.builder()
                .svgFileName(svgFile.getFileName().toString()).
                jsonFileName(jsonFile.getFileName().toString()).
                jsonContent(diagramMetadataConverter.convertToEntityAttribute(Files.readString(jsonFile, StandardCharsets.UTF_8))).
                svgContent(Files.readString(svgFile, StandardCharsets.UTF_8)).build();
       
    }

    public Path getZipFiles(String baseName) throws IOException {
        Path storageDir = Paths.get(CGMES_STORAGE_DIR);

        Path zipFile = storageDir.resolve(baseName + ZIP_EXTENSION);

        validateFileExists(zipFile, "ZIP");

        return  zipFile;

    }

    public DiagramFiles readModifiedDiagramFiles(Path outputDir, String id) throws IOException {
        Path svgFile = outputDir.resolve(id  + SVG_EXTENSION);
        Path jsonFile = outputDir.resolve(id + "_metadata" + JSON_EXTENSION);

        validateFileExists(svgFile, "Modified SVG");
        validateFileExists(jsonFile, "Modified JSON");

        return DiagramFiles.builder()
                .svgFileName(svgFile.getFileName().toString())
                .jsonFileName(jsonFile.getFileName().toString())
                .jsonContent(diagramMetadataConverter.convertToEntityAttribute(Files.readString(jsonFile)))
                .svgContent(Files.readString(svgFile)).build();
    }


    public void validateFileExists(Path file, String fileType) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException(fileType + " file not found: " + file);
        }
    }

    public DiagramModel createDiagramModel(String id, String name, DiagramFiles files, String type) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("OG : " + files.getJsonContent());
        System.out.println("Changed : " + objectMapper.writeValueAsString(files.getJsonContent()));
        DiagramModel diagramModel =  DiagramModel.builder()
                .id(id)
                .name(name)
                .svgData(files.getSvgContent())
                .metadata(files.getJsonContent())
                .diagramType(type)
                .build();

        System.out.println(diagramModel.getMetadata());
        return diagramModel;
    }

//    public byte[] createZipFromDiagramFiles(DiagramFiles files) throws IOException {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
//            addZipEntry(zos, files.getSvgFileName(), files.getSvgContentBytes());
//            addZipEntry(zos, files.getJsonFileName(), files.getJsonContentBytes());
//        }
//        return baos.toByteArray();
//    }

    public void addZipEntry(ZipOutputStream zos, String fileName, byte[] content) throws IOException {
        zos.putNextEntry(new ZipEntry(fileName));
        zos.write(content);
        zos.closeEntry();
    }

    public ResponseEntity<byte[]> createZipResponse(byte[] zipContent, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipContent);
    }

    public long generateRandomNumber() {
        return Instant.now().toEpochMilli() + ThreadLocalRandom.current().nextInt(10000);
    }
}