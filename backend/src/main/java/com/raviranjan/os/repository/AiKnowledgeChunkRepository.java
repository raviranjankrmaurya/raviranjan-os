package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.AiKnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiKnowledgeChunkRepository extends JpaRepository<AiKnowledgeChunk, UUID> {

    List<AiKnowledgeChunk> findByAiVisibleTrue();

    @Query("SELECT c FROM AiKnowledgeChunk c WHERE c.aiVisible = true AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(c.content) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<AiKnowledgeChunk> searchVisibleChunks(@Param("term") String term);
}
