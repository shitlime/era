package com.shitlime.era.handle.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 控制 Playwright
 */
@Component
public class PlaywrightHandle {
    private Playwright playwright;
    private Browser browser;

    @PostConstruct
    public void init() {
        this.playwright = Playwright.create();
        this.browser = playwright.firefox().launch();
    }

    public Response navigate(Page page, File file) {
        return page.navigate("file://" + file.getAbsolutePath(),
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    public Response navigate(Page page, String url) {
        return page.navigate(url,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    public Page newPage() {
        return this.browser.newPage();
    }

    public void closeBrowser() {
        this.browser.close();
    }

    public void closePlaywright() {
        this.playwright.close();
    }
}
