package com.temporedata.guard.modules.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * 本地磁盘存储实现(默认): ./data/files 下按 key 存文件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final StorageProperties props;

    private Path root() {
        return Paths.get(props.getLocalDir()).toAbsolutePath().normalize();
    }

    private Path resolve(String key) {
        return root().resolve(key).normalize();
    }

    @Override
    public String store(String key, InputStream in, long size) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("本地存储写入失败: " + key, e);
        }
    }

    @Override
    public boolean read(String key, OutputStream out) {
        try {
            Path p = resolve(key);
            if (!Files.exists(p)) return false;
            Files.copy(p, out);
            return true;
        } catch (IOException e) {
            log.warn("本地存储读取失败: {}", key, e);
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("本地存储删除失败: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }
}
