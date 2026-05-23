package com.yupi.yuaicodemother.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

/**
 * Web page screenshot utilities.
 */
@Slf4j
public class WebScreenshotUtils {

    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration IMPLICIT_WAIT_TIMEOUT = Duration.ofSeconds(10);

    private WebScreenshotUtils() {
    }

    /**
     * Generate and save a compressed screenshot for the given page.
     *
     * @param webUrl target url
     * @return compressed screenshot path, or {@code null} on failure
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("Web page screenshot failed: url is blank");
            return null;
        }

        String rootPath = System.getProperty("user.dir") + "/tmp/screenshots/" + UUID.randomUUID().toString().substring(0, 8);
        String browserProfilePath = System.getProperty("java.io.tmpdir") + File.separator + "chrome-profile-" + UUID.randomUUID();
        WebDriver webDriver = null;

        try {
            FileUtil.mkdir(rootPath);
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + ".png";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + "_compressed.jpg";

            webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT, browserProfilePath);
            webDriver.get(webUrl);
            waitForPageLoad(webDriver);

            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            saveImage(screenshotBytes, imageSavePath);
            compressImage(imageSavePath, compressedImagePath);
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("Web page screenshot failed: {}", webUrl, e);
            return null;
        } finally {
            quitQuietly(webDriver);
            cleanupBrowserProfile(browserProfilePath);
        }
    }

    private static WebDriver initChromeDriver(int width, int height, String browserProfilePath) {
        WebDriverManager.chromedriver().setup();
        try {
            return createChromeDriver(width, height, browserProfilePath, true);
        } catch (Exception e) {
            log.warn("Chrome startup failed with the default headless mode, retrying with the legacy mode", e);
            try {
                return createChromeDriver(width, height, browserProfilePath, false);
            } catch (Exception retryException) {
                log.error("Chrome startup failed", retryException);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to start Chrome for screenshots");
            }
        }
    }

    private static WebDriver createChromeDriver(int width, int height, String browserProfilePath, boolean useNewHeadless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(useNewHeadless ? "--headless=new" : "--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-background-networking");
        options.addArguments("--hide-scrollbars");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--remote-debugging-pipe");
        options.addArguments("--force-device-scale-factor=1");
        options.addArguments(String.format("--window-size=%d,%d", width, height));
        if (StrUtil.isNotBlank(browserProfilePath)) {
            FileUtil.mkdir(browserProfilePath);
            options.addArguments("--user-data-dir=" + browserProfilePath);
        }
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT_TIMEOUT);
        return driver;
    }

    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("Failed to save screenshot: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save screenshot");
        }
    }

    private static void compressImage(String originImagePath, String compressedImagePath) {
        final float compressionQuality = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originImagePath),
                    FileUtil.file(compressedImagePath),
                    compressionQuality
            );
        } catch (Exception e) {
            log.error("Failed to compress screenshot: {} -> {}", originImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to compress screenshot");
        }
    }

    private static void waitForPageLoad(WebDriver webDriver) {
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            wait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete"));
            Thread.sleep(2000);
        } catch (Exception e) {
            log.warn("Waiting for page load timed out, screenshot will continue", e);
        }
    }

    private static void quitQuietly(WebDriver webDriver) {
        if (webDriver == null) {
            return;
        }
        try {
            webDriver.quit();
        } catch (Exception e) {
            log.warn("Failed to close Chrome after screenshot", e);
        }
    }

    private static void cleanupBrowserProfile(String browserProfilePath) {
        if (StrUtil.isBlank(browserProfilePath)) {
            return;
        }
        try {
            FileUtil.del(browserProfilePath);
        } catch (Exception e) {
            log.warn("Failed to clean temporary Chrome profile: {}", browserProfilePath, e);
        }
    }
}
