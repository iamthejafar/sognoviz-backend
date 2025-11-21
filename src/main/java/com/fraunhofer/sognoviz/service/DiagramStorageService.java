package com.fraunhofer.sognoviz.service;

import com.fraunhofer.sognoviz.entity.DiagramEntity;
import com.fraunhofer.sognoviz.model.DiagramModel;
import com.fraunhofer.sognoviz.repository.DiagramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DiagramStorageService {

    @Autowired
    private DiagramRepository diagramRepository;

    /**
     * Save a new diagram or update existing one by name
     */
    @Transactional
    public DiagramModel saveDiagram(DiagramModel diagram) {
        DiagramEntity entity = diagramRepository.findByName(diagram.getId()).orElse(null);

        if (entity == null) {
            entity = new DiagramEntity();
            entity.setId(diagram.getId());
            entity.setCreatedAt(LocalDateTime.now());
        }

        entity.setName(diagram.getName());
        entity.setSvgData(diagram.getSvgData());
        entity.setMetadata(diagram.getMetadata());
        entity.setDiagramType(diagram.getDiagramType());
        entity.setUpdatedAt(LocalDateTime.now());
        DiagramEntity saved = diagramRepository.save(entity);
        return convertToModel(saved);
    }

    /**
     * Load diagram by ID
     */
    public DiagramModel loadDiagram(String id) {
        DiagramEntity entity = null;

        for (DiagramEntity e : diagramRepository.findAll()) {
            if (e.getId().equals(id)) {
                entity = e;
                break;
            }
        }

        if (entity == null) {
            throw new RuntimeException("Diagram not found with id: " + id);
        }

        return convertToModel(entity);
    }


    /**
     * Load diagram by name
     */
    public DiagramModel loadDiagramByName(String name) {
        DiagramEntity entity = diagramRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Diagram not found with name: " + name));
        return convertToModel(entity);
    }

    /**
     * List all diagrams
     */
    public List<DiagramModel> listAllDiagrams() {
        return diagramRepository.findAll().stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    /**
     * Delete diagram by ID
     */
    @Transactional
    public void deleteDiagram(String id) {
        if (!diagramRepository.existsByName(id)) {
            throw new RuntimeException("Diagram not found with id: " + id);
        }
        diagramRepository.deleteByName(id);
    }

    /**
     * Delete diagram by name
     */
    @Transactional
    public void deleteDiagramByName(String name) {
        if (!diagramRepository.existsByName(name)) {
            throw new RuntimeException("Diagram not found with name: " + name);
        }
        diagramRepository.deleteByName(name);
    }

    /**
     * Check if diagram exists by name
     */
    public boolean existsByName(String name) {
        return diagramRepository.existsByName(name);
    }




    /**
     * Convert entity to model
     */
    private DiagramModel convertToModel(DiagramEntity entity) {
        return new DiagramModel(
                entity.getId(),
                entity.getName(),
                entity.getSvgData(),
                entity.getMetadata(),
                entity.getDiagramType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}