/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.message.service;
import com.temporedata.guard.modules.security.message.entity.MessageEntity;
import com.temporedata.guard.modules.security.message.repository.MessageRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private final MessageRepository messageRepository;

    public List<MessageEntity> listByUser(String userId, String tenantId) {
        return this.messageRepository.findByUserIdAndTenantIdOrderByCreateTimeDesc(userId, tenantId);
    }

    public long unreadCount(String userId) {
        return this.messageRepository.countByUserIdAndIsRead(userId, 0);
    }

    @Transactional
    public MessageEntity send(String userId, String type, String title, String content, String bizType, String bizId, String tenantId) {
        MessageEntity entity = new MessageEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setIsRead(0);
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return (MessageEntity)this.messageRepository.save(entity);
    }

    @Transactional
    public void markRead(String id) {
        MessageEntity entity = (MessageEntity)this.messageRepository.findById(id).orElseThrow(() -> new BusinessException("\u6d88\u606f\u4e0d\u5b58\u5728"));
        entity.setIsRead(1);
        this.messageRepository.save(entity);
    }

    @Transactional
    public void markAllRead(String userId) {
        List<MessageEntity> unread = this.messageRepository.findByUserIdAndIsRead(userId, 0);
        for (MessageEntity m : unread) {
            m.setIsRead(1);
        }
        this.messageRepository.saveAll(unread);
    }

    @Transactional
    public void delete(String id) {
        this.messageRepository.deleteById(id);
    }

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }
}

