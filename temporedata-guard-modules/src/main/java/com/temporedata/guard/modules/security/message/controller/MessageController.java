/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.message.controller;
import com.temporedata.guard.modules.security.message.entity.MessageEntity;
import com.temporedata.guard.modules.security.message.service.MessageService;



import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/message"})
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    public List<MessageEntity> list(@RequestParam String userId, @RequestParam(defaultValue="DEFAULT") String tenantId) {
        return this.messageService.listByUser(userId, tenantId);
    }

    @GetMapping(value={"/unread-count"})
    public Map<String, Long> unreadCount(@RequestParam String userId) {
        return Map.of("count", this.messageService.unreadCount(userId));
    }

    @PostMapping
    public MessageEntity send(@RequestBody Map<String, String> body) {
        return this.messageService.send(body.get("userId"), body.get("type"), body.get("title"), body.get("content"), body.get("bizType"), body.get("bizId"), body.getOrDefault("tenantId", "DEFAULT"));
    }

    @PutMapping(value={"/{id}/read"})
    public void markRead(@PathVariable String id) {
        this.messageService.markRead(id);
    }

    @PutMapping(value={"/read-all"})
    public void markAllRead(@RequestParam String userId) {
        this.messageService.markAllRead(userId);
    }

    @DeleteMapping(value={"/{id}"})
    public void delete(@PathVariable String id) {
        this.messageService.delete(id);
    }

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }
}

