package com.yupi.yuaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.manager.CosManager;
import com.yupi.yuaicodemother.service.ScreenshotService;
import com.yupi.yuaicodemother.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "Screenshot url cannot be blank");
        log.info("Start generating web screenshot, url={}", webUrl);

        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "Failed to generate screenshot");

        try {
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "Failed to upload screenshot to COS");
            log.info("Screenshot uploaded successfully, url={}", cosUrl);
            return cosUrl;
        } finally {
            cleanupLocalFile(localScreenshotPath);
        }
    }

    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("Screenshot file does not exist: {}", localScreenshotPath);
            return null;
        }
        String cosKey = generateScreenshotKey(screenshotFile.getName());
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String safeFileName = StrUtil.blankToDefault(FileUtil.getName(fileName), "screenshot.jpg");
        return String.format("screenshots/%s/%s", datePath, safeFileName);
    }

    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            FileUtil.del(localFile);
            log.info("Deleted local screenshot file: {}", localFilePath);
        }
    }
}
