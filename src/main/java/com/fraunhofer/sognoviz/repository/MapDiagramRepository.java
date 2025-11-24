package com.fraunhofer.sognoviz.repository;

import com.fraunhofer.sognoviz.entity.NetworkMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MapDiagramRepository extends JpaRepository<NetworkMapEntity, Long> {

    Optional<NetworkMapEntity> findByName(String name);

    boolean existsByName(String name);

    void deleteByName(String name);
}