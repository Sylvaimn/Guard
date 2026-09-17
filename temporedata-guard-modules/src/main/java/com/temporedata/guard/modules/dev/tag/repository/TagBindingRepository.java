package com.temporedata.guard.modules.dev.tag.repository;

import com.temporedata.guard.modules.dev.tag.entity.TagBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagBindingRepository extends JpaRepository<TagBindingEntity, String> {

    List<TagBindingEntity> findByAssetTypeAndAssetId(String assetType, String assetId);

    List<TagBindingEntity> findByAssetTypeAndAssetIdIn(String assetType, List<String> assetIds);

    List<TagBindingEntity> findByTagIdAndAssetTypeAndAssetId(String tagId, String assetType, String assetId);

    void deleteByTagIdAndAssetTypeAndAssetId(String tagId, String assetType, String assetId);

    void deleteByTagId(String tagId);

    void deleteByAssetTypeAndAssetId(String assetType, String assetId);
}
