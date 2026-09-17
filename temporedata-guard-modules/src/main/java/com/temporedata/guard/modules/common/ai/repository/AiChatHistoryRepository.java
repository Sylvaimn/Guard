package com.temporedata.guard.modules.common.ai.repository;

import com.temporedata.guard.modules.common.ai.entity.AiChatHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AI chat history repository.
 */
public interface AiChatHistoryRepository extends JpaRepository<AiChatHistoryEntity, String> {

    List<AiChatHistoryEntity> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    List<AiChatHistoryEntity> findByUserIdOrderByCreateTimeDesc(String userId);

    long countBySessionIdAndRoleAndReadFlagFalse(String sessionId, String role);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update AiChatHistoryEntity h set h.readFlag = true where h.sessionId = :sessionId")
    int markReadBySession(@org.springframework.data.repository.query.Param("sessionId") String sessionId);
}