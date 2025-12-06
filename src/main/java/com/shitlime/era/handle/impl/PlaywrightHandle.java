package com.shitlime.era.handle.impl;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.shitlime.psj.Stealth;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * 控制 Playwright
 */
@Slf4j
@Component
public class PlaywrightHandle {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    @PostConstruct
    public void init() {
        this.playwright = Playwright.create();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
        launchOptions.setHeadless(true);
        launchOptions.setArgs(List.of(
                "--disable-gpu",              // 关闭 GPU 加速
                "--disable-dev-shm-usage",              // 避免共享内存问题（特别是 Docker 中）
                "--disable-extensions",                 // 不加载扩展
                "--disable-background-networking",
                "--disable-default-apps",
                "--disable-sync",
                "--disable-translate",
                "--hide-scrollbars",
                "--metrics-recording-only",
                "--mute-audio",
                "--no-first-run",
                "--safebrowsing-disable-auto-update"
        ));
        launchOptions.setTimeout(25000);
        this.browser = playwright.chromium().launch(launchOptions);
        this.context = this.browser.newContext(new Browser.NewContextOptions()
                .setLocale("zh-CN")
                .setTimezoneId("Asia/Shanghai")
                .setViewportSize(1280, 720)
        );

        // 注册 JVM 退出钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook: closing browser and playwright...");
            try {
                if (browser != null) browser.close();
            } catch (PlaywrightException e) {
                if (e.getMessage().contains("Playwright connection closed")) {
                    System.out.println("Browser already disconnected, skipping browser.close()");
                } else {
                    e.printStackTrace();
                }
            }

            try {
                if (playwright != null) playwright.close();
            } catch (PlaywrightException e) {
                if (e.getMessage().contains("Playwright connection closed")) {
                    System.out.println("Playwright already disconnected, skipping playwright.close()");
                } else {
                    e.printStackTrace();
                }
            }
        }));
    }

    @PreDestroy
    public void shutdown() {
        if (browser != null) closeBrowser();
        if (playwright != null) closePlaywright();
    }

    public Response navigate(Page page, File file) {
        return page.navigate("file://" + file.getAbsolutePath(),
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    public Response navigate(Page page, String url) {
        return page.navigate(url,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    public Response noFailNavigate(Page page, String url) {
        try {
            return page.navigate(url,
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(15000));
        } catch (TimeoutError e) {
            log.warn("页面超时：{}", e.toString());
            return null;
        }
    }

    public Page newPage() {
        Page page = this.context.newPage();
        return page;
    }

    public Page newStealthPage() {
        Page page = this.context.newPage();
        Stealth.StealthConfig stealthConfig = new Stealth.StealthConfig();
        stealthConfig.navigatorLanguagesOverride = List.of("zh-CN", "zh");
        Stealth stealth = new Stealth(stealthConfig);
        stealth.applyStealth(page);
        return page;
    }

    public void closeBrowser() {
        this.context.close();
        this.browser.close();
    }

    public void closePlaywright() {
        this.playwright.close();
    }
}
