package com.fraunhofer.sognoviz.repository;

import com.fraunhofer.sognoviz.entity.DiagramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiagramRepository extends JpaRepository<DiagramEntity, Long> {



    Optional<DiagramEntity> findByName(String name);

    boolean existsByName(String name);

    void deleteByName(String name);
}