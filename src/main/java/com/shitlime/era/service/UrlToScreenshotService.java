package com.shitlime.era.service;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.ScreenshotCaret;
import com.microsoft.playwright.options.ScreenshotType;
import com.shitlime.era.handle.impl.PlaywrightHandle;
import com.shitlime.era.mapper.UrlToScreenshotMapper;
import com.shitlime.era.pojo.dto.UrlToScreenshotDTO;
import com.shitlime.era.pojo.entry.UrlToScreenshot;
import com.shitlime.era.utils.TableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UrlToScreenshotService {
    @Autowired
    PlaywrightHandle playwrightHandle;
    @Autowired
    UrlToScreenshotMapper urlToScreenshotMapper;
    @Autowired
    TableUtils tableUtils;

    /**
     * 根据 url 访问并返回截图
     *
     * @param url
     * @return
     */
    public synchronized byte[] getScreenshot(String url) {
        try (Page page = playwrightHandle.newStealthPage()) {
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

    /**
     * 添加 Url 自动截图规则
     * @param urlToScreenshotDTO
     * @return
     */
    public boolean addRule(UrlToScreenshotDTO urlToScreenshotDTO) {
        UrlToScreenshot urlToScreenshot = new UrlToScreenshot();
        BeanUtils.copyProperties(urlToScreenshotDTO, urlToScreenshot);
        if (tableUtils.isExist(UrlToScreenshotMapper.tableName)) {
            // 已有该规则时跳过
            UrlToScreenshot us = new UrlToScreenshot();
            us.setUserId(urlToScreenshotDTO.getUserId());
            us.setGroupId(urlToScreenshotDTO.getGroupId());
            us.setDomainName(urlToScreenshotDTO.getDomainName());
            List<UrlToScreenshot> urlToScreenshotList = urlToScreenshotMapper.select(us);
            if (urlToScreenshotList != null && !urlToScreenshotList.isEmpty()) {
                return false;
            }
        } else {
            urlToScreenshotMapper.createTable();
        }

        // 插入新的规则
        urlToScreenshot.setCreateTime(LocalDateTime.now());
        urlToScreenshot.setUpdateTime(LocalDateTime.now());
        urlToScreenshot.setEnable(true);
        urlToScreenshotMapper.insert(urlToScreenshot);
        return true;
    }

    /**
     * 查询 Url 自动截图规则
     * @param groupId
     * @param userId
     * @return
     */
    public List<UrlToScreenshot> getRule(Long groupId, Long userId) {
        if (!tableUtils.isExist(UrlToScreenshotMapper.tableName)) {
            return null;
        }

        if (groupId != null) {
            // 群内规则。不需要匹配用户id
            userId = null;
        }
        UrlToScreenshot us = new UrlToScreenshot();
        us.setUserId(userId);
        us.setGroupId(groupId);
        return urlToScreenshotMapper.select(us);
    }

    /**
     * 根据数据库 id 删除 Url 自动截图规则
     * @param id
     * @return
     */
    public boolean deleteRule(Long id) {
        if (!tableUtils.isExist(UrlToScreenshotMapper.tableName)) {
            // 数据库表不存在
            return false;
        }

        urlToScreenshotMapper.delete(id);
        return true;
    }

    /**
     * 启用/停用 Url 自动截图规则
     * @param id
     * @return
     */
    public boolean toggleRuleEnable(Long id, boolean enable) {
        if (enable) {
            urlToScreenshotMapper.disable(id);
        } else {
            urlToScreenshotMapper.enable(id);
        }
        return true;
    }

    /**
     * 检查规则是否存在
     * @param urlToScreenshotDTO
     * @return
     */
    public boolean checkRule(UrlToScreenshotDTO urlToScreenshotDTO) {
        if (!tableUtils.isExist(UrlToScreenshotMapper.tableName)) {
            return false;
        }

        if (urlToScreenshotDTO.getGroupId() != null) {
            // 群内规则。不需要匹配用户id
            urlToScreenshotDTO.setUserId(null);
        }
        UrlToScreenshot us = new UrlToScreenshot();
        us.setDomainName(urlToScreenshotDTO.getDomainName());
        us.setUserId(urlToScreenshotDTO.getUserId());
        us.setGroupId(urlToScreenshotDTO.getGroupId());
        us.setEnable(true);
        List<UrlToScreenshot> result = urlToScreenshotMapper.select(us);
        return result != null && !result.isEmpty();
    }
}
