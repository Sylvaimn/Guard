package com.temporedata.guard.modules.data.datasource.plugin;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据源插件注册表: 收集所有 DataSourcePlugin Bean, 按 type() 建立路由表。
 * 新增类型 = 新增插件类, 注册表与主流程零改动。
 */
@Slf4j
@Component
public class PluginRegistry {

    private final List<DataSourcePlugin> plugins;
    private Map<String, DataSourcePlugin> byType = new ConcurrentHashMap<>();

    public PluginRegistry(List<DataSourcePlugin> plugins) {
        this.plugins = plugins;
    }

    @PostConstruct
    public void init() {
        byType = new ConcurrentHashMap<>();
        for (DataSourcePlugin p : plugins) {
            byType.put(p.type(), p);
            p.aliases().forEach(a -> byType.putIfAbsent(a, p));
        }
        log.info("[PLUGIN] 数据源插件注册: {}", String.join(", ", byType.keySet()));
    }

    public DataSourcePlugin get(String type) {
        DataSourcePlugin p = byType.get(type);
        if (p == null) {
            throw new BusinessException("未注册的数据源插件类型: " + type + "（可用: " + String.join(", ", byType.keySet()) + "）");
        }
        return p;
    }

    public List<String> types() {
        return List.copyOf(byType.keySet());
    }

    public Map<String, String[]> capabilities() {
        return plugins.stream().collect(Collectors.toMap(DataSourcePlugin::type, DataSourcePlugin::capabilities, (a, b) -> a));
    }
}
