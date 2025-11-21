package com.fraunhofer.sognoviz.controller;

import com.fraunhofer.sognoviz.model.DiagramFiles;
import com.fraunhofer.sognoviz.model.DiagramModel;
import com.fraunhofer.sognoviz.service.DiagramGeneratorService;
import com.fraunhofer.sognoviz.service.DiagramStorageService;
import com.fraunhofer.sognoviz.util.DiagramFileHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/diagrams")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173}")
@RequiredArgsConstructor
public class DiagramController {

    private final DiagramGeneratorService diagramGeneratorService;
    private final DiagramStorageService diagramStorageService;
    private final DiagramFileHelper fileHelper;


    @PostMapping("/nad")
    public ResponseEntity<DiagramModel> generateNadDiagram(
            @RequestParam("file") MultipartFile file) {
        String id = UUID.randomUUID().toString();
        try {
            log.info("Generating NAD diagram for id: {}", id);

            String fileName = "nad_" + id;

            Path storedFile = fileHelper.storeUploadedFile(file, fileName);
            Path outputDir = diagramGeneratorService.generateNAD(storedFile.toString());
            DiagramFiles diagramFiles = fileHelper.readDiagramFiles(outputDir, "network");

            DiagramModel diagram = fileHelper.createDiagramModel(
                    id,
                    fileName,
                    diagramFiles,
                    "NAD"
            );
            diagram = diagramStorageService.saveDiagram(diagram);



            log.info("Successfully generated NAD diagram for id: {}", id);

            return ResponseEntity.ok()
                    .body(diagram);


        } catch (IOException e) {
            log.error("Failed to generate NAD diagram for id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/nad/{id}")
    public ResponseEntity<?> getNadData(@PathVariable String id) {
        log.debug("Fetching NAD diagram data for id: {}", id);
        return getDiagramByIdAndType(id, "NAD");
    }

    @PostMapping("/sld/selectionData")
    public ResponseEntity<Map<String, Object>> getSldSelectionData(
            @RequestParam("file") MultipartFile file) {
        String id = UUID.randomUUID().toString();

        try {
            String fileName = "sld_" + id;

            Path storedFile = fileHelper.storeUploadedFile(file, fileName);

            Map<String, Object> sldData = diagramGeneratorService.getSldData(storedFile.toString());

            Map<String, Object> responseData = new HashMap<>();

            responseData.put("id", id);
            responseData.put("sldData", sldData);

            return ResponseEntity.ok(responseData);

        } catch (IOException e) {
            log.error("Failed to get SLD selection data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/sld")
    public ResponseEntity<DiagramModel> generateSldDiagram(
            @RequestParam("type") String type,
            @RequestParam(value = "selectionId", required = false) String selectionId,
            @RequestParam("id") String id) {

        try {
            log.info("Generating SLD diagram for id: {}, type: {}", id, type);

            String fileName = "sld_" + id;

            Path cgmesFile = fileHelper.getZipFiles(fileName);

            Path outputDir = diagramGeneratorService.generateSLD(cgmesFile.toString(), type, selectionId);
            DiagramFiles diagramFiles = fileHelper.readDiagramFiles(outputDir, "sld");

            DiagramModel diagram = fileHelper.createDiagramModel(
                    id,
                    fileName,
                    diagramFiles,
                    "SLD"
            );
            diagram = diagramStorageService.saveDiagram(diagram);

            log.info("Successfully generated SLD diagram for id: {}", id);
            return ResponseEntity.ok()
                    .body(diagram);

        } catch (IOException e) {
            log.error("Failed to generate SLD diagram for id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/sld/{id}")
    public ResponseEntity<?> getSldData(@PathVariable String id) {
        log.debug("Fetching SLD diagram data for id: {}", id);
        return getDiagramByIdAndType(id, "SLD");
    }

    @GetMapping
    public ResponseEntity<List<DiagramModel>> getAllDiagrams() {
        try {
            log.debug("Fetching all diagrams");
            List<DiagramModel> diagrams = diagramStorageService.listAllDiagrams();

            return ResponseEntity.ok()
                    .body(diagrams);


        } catch (Exception e) {
            log.error("Failed to list all diagrams", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDiagramById(@PathVariable String id) {
        try {
            log.debug("Fetching diagram with id: {}", id);
            DiagramModel diagram = diagramStorageService.loadDiagram(id);
            return ResponseEntity.ok(diagram);

        } catch (RuntimeException e) {
            log.error("Diagram not found with id: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to load diagram with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<?> getDiagramByName(@PathVariable String name) {
        try {
            log.debug("Fetching diagram with name: {}", name);
            DiagramModel diagram = diagramStorageService.loadDiagramByName(name);
            return ResponseEntity.ok(diagram);

        } catch (RuntimeException e) {
            log.error("Diagram not found with name: {}", name, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to load diagram with name: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDiagram(
            @PathVariable String id,
            @RequestBody DiagramModel diagram) {

        try {
            log.info("Updating diagram with id: {}", id);

            DiagramModel oldModel = diagramStorageService.loadDiagram(id);
            diagram.setId(id);
            DiagramModel updated = diagramStorageService.saveDiagram(diagram);

            DiagramFileHelper.updateFileName(oldModel.getName(),updated.getName(),".zip");
            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            log.error("Diagram not found with id: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to update diagram with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiagramById(@PathVariable String id) {
        try {
            log.info("Deleting diagram with id: {}", id);
            diagramStorageService.deleteDiagram(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            log.error("Diagram not found with id: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete diagram with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/name/{name}")
    public ResponseEntity<Void> deleteDiagramByName(@PathVariable String name) {
        try {
            log.info("Deleting diagram with name: {}", name);
            diagramStorageService.deleteDiagramByName(name);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            log.error("Diagram not found with name: {}", name, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete diagram with name: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<?> getDiagramByIdAndType(String id, String expectedType) {
        try {
            DiagramModel diagram = diagramStorageService.loadDiagram(id);
            if (!expectedType.equals(diagram.getDiagramType())) {
                return ResponseEntity.badRequest()
                        .body("Diagram with id " + id + " is not a " + expectedType + " diagram");
            }
            return ResponseEntity.ok(diagram);

        } catch (RuntimeException e) {
            log.error("{} diagram not found with id: {}", expectedType, id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to load {} diagram with id: {}", expectedType, id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}