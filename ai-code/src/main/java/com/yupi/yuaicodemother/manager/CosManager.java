package com.yupi.yuaicodemother.manager;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.yupi.yuaicodemother.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS object storage manager.
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    public PutObjectResult putObject(String key, File file) {
        String normalizedKey = normalizeKey(key);
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), normalizedKey, file);
        return cosClient.putObject(putObjectRequest);
    }

    public String uploadFile(String key, File file) {
        String normalizedKey = normalizeKey(key);
        try {
            PutObjectResult result = putObject(normalizedKey, file);
            if (result == null) {
                log.error("COS upload returned null result, file={}, key={}", file.getName(), normalizedKey);
                return null;
            }
            String url = buildFileUrl(normalizedKey);
            log.info("File uploaded to COS successfully: {} -> {}", file.getName(), url);
            return url;
        } catch (CosServiceException e) {
            log.error(
                    "COS upload failed, file={}, key={}, statusCode={}, errorCode={}, requestId={}",
                    file.getName(),
                    normalizedKey,
                    e.getStatusCode(),
                    e.getErrorCode(),
                    e.getRequestId(),
                    e
            );
            throw e;
        }
    }

    static String normalizeKey(String key) {
        String normalizedKey = StrUtil.blankToDefault(key, "");
        return StrUtil.removePrefix(normalizedKey, "/");
    }

    static String joinHostAndKey(String host, String key) {
        String normalizedHost = StrUtil.removeSuffix(StrUtil.blankToDefault(host, ""), "/");
        String normalizedKey = normalizeKey(key);
        if (StrUtil.isBlank(normalizedHost)) {
            return normalizedKey;
        }
        if (StrUtil.isBlank(normalizedKey)) {
            return normalizedHost;
        }
        return normalizedHost + "/" + normalizedKey;
    }

    private String buildFileUrl(String key) {
        String host = cosClientConfig.getHost();
        if (StrUtil.isNotBlank(host)) {
            return joinHostAndKey(host, key);
        }
        return cosClient.getObjectUrl(cosClientConfig.getBucket(), key).toString();
    }
}
