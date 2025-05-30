package com.shitlime.era.service;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.ScreenshotCaret;
import com.microsoft.playwright.options.ScreenshotType;
import com.shitlime.era.handle.impl.PlaywrightHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UrlToScreenshotService {
    @Autowired
    PlaywrightHandle playwrightHandle;

    /**
     * 根据 url 访问并返回截图
     *
     * @param url
     * @return
     */
    public synchronized byte[] getScreenshot(String url) {
        try (Page page = playwrightHandle.newPage()) {
            log.info("访问Url：{}", url);
            playwrightHandle.noFailNavigate(page, url);
            log.info("进行截图…");
            return page.screenshot(
                    new Page.ScreenshotOptions()
                            .setFullPage(true)
                            .setType(ScreenshotType.JPEG)
                            .setQuality(80)    // JPEG图片质量。 经测试：0不可用/50糊，不如80/80微糊，建议使用/100边界微糊，不如PNG
                            .setAnimations(ScreenshotAnimations.DISABLED)
                            .setCaret(ScreenshotCaret.HIDE)
                            .setTimeout(45000)
            );
        } catch (RuntimeException e) {
            log.error(e.toString());
        }
        return null;
    }

    //todo 权限控制、数据库写入
    // 1. 记录群组的配置： 哪些域名的网站可以自动解析
    // 2. 权限： 群主、管理员可以扩充配置
}
