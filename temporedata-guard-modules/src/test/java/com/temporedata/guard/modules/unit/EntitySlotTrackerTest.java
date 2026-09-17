package com.temporedata.guard.modules.unit;

import com.temporedata.guard.modules.agent.aianalysis.service.EntitySlotTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 会话槽位(指代消解)单测: null 防护/代词替换/槽位上限 */
class EntitySlotTrackerTest {

    private EntitySlotTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new EntitySlotTracker(Mockito.mock(com.temporedata.guard.modules.police.intelligence.service.IntelligenceService.class));
    }

    @Test
    void resolveWithNullSessionReturnsText() {
        assertEquals("查一下他的轨迹", tracker.resolve(null, "查一下他的轨迹"));
    }

    @Test
    void resolveWithNullTextIsSafe() {
        assertNull(tracker.resolve("s1", null));
    }

    @Test
    void pronounReplacedByObservedEntity() {
        tracker.observe("s1", List.of("张某某"));
        String out = tracker.resolve("s1", "查一下他的轨迹");
        assertTrue(out.contains("张某某"));
        assertFalse(out.contains("他"));
    }

    @Test
    void emptySlotLeavesTextUnchanged() {
        assertEquals("你好", tracker.resolve("s-empty", "你好"));
    }
}
