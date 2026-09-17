package com.temporedata.guard.modules.police.intelligence.service;

import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;

import java.util.List;

/**
 * 情报检索引擎抽象：默认数据库实现(LIKE/JPQL)，可插拔 ES/OpenSearch 全文检索。
 * 数据量增长后切换 zy.search.engine=elasticsearch 即可，业务层无感。
 */
public interface SearchEngine {

    String name();

    /** 关键字检索(姓名/主标识), 租户 + 密级下推 */
    List<EntityMasterEntity> searchByKeyword(String tenantId, List<String> densities, String keyword, int limit);

    /** 标签码精确检索 */
    List<EntityMasterEntity> searchByLabelCode(String tenantId, List<String> densities, String labelCode, int limit);
}
