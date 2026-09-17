package com.temporedata.guard.modules.police.photo.repository;

import com.temporedata.guard.modules.police.photo.entity.PersonPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonPhotoRepository extends JpaRepository<PersonPhotoEntity, String> {
    List<PersonPhotoEntity> findByEntityIdOrderByCreateTimeDesc(String entityId);
}
