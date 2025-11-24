package com.fraunhofer.sognoviz.service;

import com.fraunhofer.sognoviz.entity.NetworkMapEntity;
import com.fraunhofer.sognoviz.model.NetworkMapModel;
import com.fraunhofer.sognoviz.repository.MapDiagramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MapDiagramStorageService {

    @Autowired
    private MapDiagramRepository mapDiagramRepository;

    /**
     * Save a new map diagram or update existing one by name
     */
    @Transactional
    public NetworkMapModel saveMapDiagram(NetworkMapModel mapDiagram) {
        NetworkMapEntity entity = mapDiagramRepository.findByName(mapDiagram.getId()).orElse(null);

        if (entity == null) {
            entity = new NetworkMapEntity();
            entity.setId(mapDiagram.getId());
            entity.setCreatedAt(LocalDateTime.now());
        }

        entity.setName(mapDiagram.getName());
        entity.setSvg(mapDiagram.getSvg());
        entity.setMetadata(mapDiagram.getMetadata());
        entity.setLine(mapDiagram.getLine());
        entity.setLinePosition(mapDiagram.getLinePosition());
        entity.setSubstation(mapDiagram.getSubstation());
        entity.setSubstationPosition(mapDiagram.getSubstationPosition());
        entity.setDiagramType(mapDiagram.getDiagramType());
        entity.setUpdatedAt(LocalDateTime.now());

        NetworkMapEntity saved = mapDiagramRepository.save(entity);
        return convertToModel(saved);
    }

    /**
     * Load map diagram by ID
     */
    public NetworkMapModel loadMapDiagram(String id) {
        NetworkMapEntity entity = null;

        for (NetworkMapEntity e : mapDiagramRepository.findAll()) {
            if (e.getId().equals(id)) {
                entity = e;
                break;
            }
        }

        if (entity == null) {
            throw new RuntimeException("Map diagram not found with id: " + id);
        }

        return convertToModel(entity);
    }

    /**
     * Load map diagram by name
     */
    public NetworkMapModel loadMapDiagramByName(String name) {
        NetworkMapEntity entity = mapDiagramRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Map diagram not found with name: " + name));
        return convertToModel(entity);
    }

    /**
     * List all map diagrams
     */
    public List<NetworkMapModel> listAllMapDiagrams() {
        return mapDiagramRepository.findAll().stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    /**
     * Delete map diagram by ID
     */
    @Transactional
    public void deleteMapDiagram(String id) {
        if (!mapDiagramRepository.existsByName(id)) {
            throw new RuntimeException("Map diagram not found with id: " + id);
        }
        mapDiagramRepository.deleteByName(id);
    }

    /**
     * Delete map diagram by name
     */
    @Transactional
    public void deleteMapDiagramByName(String name) {
        if (!mapDiagramRepository.existsByName(name)) {
            throw new RuntimeException("Map diagram not found with name: " + name);
        }
        mapDiagramRepository.deleteByName(name);
    }

    /**
     * Check if map diagram exists by name
     */
    public boolean existsByName(String name) {
        return mapDiagramRepository.existsByName(name);
    }

    /**
     * Convert entity to model
     */
    private NetworkMapModel convertToModel(NetworkMapEntity entity) {
        return NetworkMapModel.builder()
                .id(entity.getId())
                .name(entity.getName())
                .svg(entity.getSvg())
                .metadata(entity.getMetadata())
                .line(entity.getLine())
                .linePosition(entity.getLinePosition())
                .substation(entity.getSubstation())
                .substationPosition(entity.getSubstationPosition())
                .diagramType(entity.getDiagramType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}