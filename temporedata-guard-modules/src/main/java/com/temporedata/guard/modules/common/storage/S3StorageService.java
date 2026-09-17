package com.temporedata.guard.modules.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 对象存储实现(S3/MinIO 兼容): 通过 HTTP REST 直连 bucket。
 * 生产建议使用云厂商 SDK(可在此类内扩展签名), 当前适配 MinIO 直连/内网对象存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final StorageProperties props;
    private final RestTemplate restTemplate;

    private String url(String key) {
        String ep = props.getEndpoint().replaceAll("/+$", "");
        return ep + "/" + props.getBucket() + "/" + key;
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        if (props.getToken() != null && !props.getToken().isEmpty()) {
            h.set("Authorization", "Bearer " + props.getToken());
        }
        return h;
    }

    @Override
    public String store(String key, InputStream in, long size) {
        try {
            byte[] data = in.readAllBytes();
            HttpEntity<byte[]> entity = new HttpEntity<>(data, headers());
            ResponseEntity<String> resp = restTemplate.exchange(url(key), HttpMethod.PUT, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("对象存储写入失败: " + resp.getStatusCode());
            }
            return url(key);
        } catch (IOException e) {
            throw new RuntimeException("对象存储写入失败: " + key, e);
        }
    }

    @Override
    public boolean read(String key, OutputStream out) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers());
            ResponseEntity<byte[]> resp = restTemplate.exchange(url(key), HttpMethod.GET, entity, byte[].class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return false;
            out.write(resp.getBody());
            return true;
        } catch (Exception e) {
            log.warn("对象存储读取失败: {}", key, e);
            return false;
        }
    }

    @Override
    public void delete(String key) {
        try {
            restTemplate.exchange(url(key), HttpMethod.DELETE, new HttpEntity<>(headers()), String.class);
        } catch (Exception e) {
            log.warn("对象存储删除失败: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers());
            ResponseEntity<byte[]> resp = restTemplate.exchange(url(key), HttpMethod.HEAD, entity, byte[].class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
