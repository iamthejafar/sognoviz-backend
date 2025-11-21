package com.fraunhofer.sognoviz.controller;

import com.fraunhofer.sognoviz.model.DiagramFiles;
import com.fraunhofer.sognoviz.model.DiagramModel;
import com.fraunhofer.sognoviz.service.DiagramGeneratorService;
import com.fraunhofer.sognoviz.service.DiagramStorageService;
import com.fraunhofer.sognoviz.util.DiagramFileHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/modifications")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173}")
@RequiredArgsConstructor
public class DiagramModificationController {

    private final DiagramGeneratorService diagramGeneratorService;
    private final DiagramStorageService diagramStorageService;

    private final DiagramFileHelper fileHelper;

    @PostMapping("/remove-connectable")
    public ResponseEntity<DiagramModel> removeConnectable(
            @RequestParam("file") MultipartFile file,
            @RequestParam("equipmentId") String equipmentId,
            @RequestParam("diagramType") String diagramType,
            @RequestParam("fileName") String fileName,
            @RequestParam("id") String id) {

        try {
            log.info("Removing connectable {} from diagram {}", equipmentId, id);


            DiagramModel diagramModel = diagramStorageService.loadDiagram(id);

            diagramGeneratorService.removeConnectable(equipmentId, diagramModel);

            Path outputDir = Paths.get(DiagramFileHelper.CGMES_STORAGE_DIR);
            DiagramFiles modifiedFiles = fileHelper.readModifiedDiagramFiles(outputDir, fileName);

            System.out.println(modifiedFiles.getSvgContent());
            System.out.println(modifiedFiles.getSvgFileName());


            DiagramModel model = fileHelper.createDiagramModel(id, fileName, modifiedFiles, diagramType);

            DiagramModel diagram = diagramStorageService.saveDiagram(
                    model
            );


            log.info("Successfully removed connectable {} from diagram {}", equipmentId, id);
            return ResponseEntity.ok()
                    .body(diagram);

        } catch (IOException e) {
            log.error("Failed to remove connectable {} from diagram {}", equipmentId, id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

