package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.DsaNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DsaNoteRepository extends JpaRepository<DsaNote, UUID> {
    List<DsaNote> findByProblemId(UUID problemId);
}
