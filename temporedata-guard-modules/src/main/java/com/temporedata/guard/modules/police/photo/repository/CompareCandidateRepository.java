package com.temporedata.guard.modules.police.photo.repository;

import com.temporedata.guard.modules.police.photo.entity.CompareCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompareCandidateRepository extends JpaRepository<CompareCandidateEntity, String> {
    List<CompareCandidateEntity> findByTaskIdOrderBySimilarityDesc(String taskId);
    List<CompareCandidateEntity> findByTaskIdAndStatus(String taskId, String status);
}
