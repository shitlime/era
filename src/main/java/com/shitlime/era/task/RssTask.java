package com.shitlime.era.task;

import com.alibaba.fastjson2.JSON;
import com.microsoft.playwright.Page;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.model.ArrayMsg;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.handle.impl.PlaywrightHandle;
import com.shitlime.era.mapper.RssSourceMapper;
import com.shitlime.era.mapper.RssSubscriptionMapper;
import com.shitlime.era.pojo.entry.RssSource;
import com.shitlime.era.pojo.entry.RssSubscription;
import com.shitlime.era.utils.TableUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class RssTask {
    private Page page;

    @Resource
    private BotContainer botContainer;
    @Autowired
    private EraConfig eraConfig;
    @Autowired
    private RssSubscriptionMapper rssSubscriptionMapper;
    @Autowired
    private RssSourceMapper rssSourceMapper;
    @Autowired
    private TableUtils tableUtils;
    @Autowired
    private PlaywrightHandle playwrightHandle;

    @Scheduled(cron = "0 */12 * * * ?")
    public void fetchRss() {
        if (!tableUtils.isExist(RssSubscriptionMapper.tableName)
                || !tableUtils.isExist(RssSourceMapper.tableName)
        ) {
            return;
        }

        log.info("执行rss订阅更新任务。");

        List<Long> sourceIds = rssSubscriptionMapper.selectAllSourceId();
        if (sourceIds == null || sourceIds.isEmpty()) {
            log.info("没有任何启用的rss订阅。");
            return;
        }
        try {
            this.page = playwrightHandle.newPage();
            List<RssSource> rssSources = rssSourceMapper.selectByIds(sourceIds);

            for (RssSource rssSource : rssSources) {
                List<String> latestFeed = JSON.parseArray(rssSource.getLatestFeed(), String.class);
                String url = rssSource.getUrl();
                SyndFeed feed;
                try {
                    URLConnection connection = new URI(url).toURL().openConnection();
                    connection.setConnectTimeout(20 * 1000);
                    connection.setReadTimeout(20 * 1000);
                    feed = new SyndFeedInput().build(new XmlReader(connection.getInputStream()));
                } catch (FeedException | IOException | URISyntaxException e) {
                    log.info(e.toString());
                    continue;
                }

                // 处理 feed entries （操作：倒序、去重）
                List<SyndEntry> entryList = new ArrayList<>();
                for (SyndEntry entry : feed.getEntries().reversed()) {
                    if (!entryList.contains(entry)) {
                        entryList.add(entry);
                    }
                }

                boolean hasUpdate = false;
                for (SyndEntry entry : entryList) {
                    if (latestFeed.stream().noneMatch(l -> l.equals(entry.getLink()))) {
                        // 如果有新的entry
                        hasUpdate = true;

                        // 构建消息
                        List<Map<String, Object>> fwmsg = buildRssMessage(rssSource, entry);

                        // 给所有订阅者发送消息
                        List<RssSubscription> rssSubscriptions = rssSubscriptionMapper
                                .selectEnableBySourceId(rssSource.getId());
                        for (RssSubscription rssSubscription : rssSubscriptions) {
                            Bot bot = botContainer.robots.get(eraConfig.getBot().getId());
                            if (rssSubscription.getGroupId() != null) {
                                bot.sendGroupForwardMsg(rssSubscription.getGroupId(), fwmsg);
                            } else {
                                bot.sendPrivateForwardMsg(rssSubscription.getUserId(), fwmsg);
                            }
                        }

                        log.info("{}有新的条目：{}。", feed.getTitle(), entry.getTitle());
                    }
                }
                if (hasUpdate) {
                    // 更新数据库
                    rssSource.setLatestFeed(JSON.toJSONString(feed.getEntries().stream()
                            .map(SyndEntry::getLink).toList()));
                    rssSource.setFetchTime(LocalDateTime.now());
                    rssSourceMapper.fetch(rssSource);
                }
            }
        } finally {
            this.page.close();
            log.info("rss订阅更新任务执行完毕。");
        }
    }

    private List<Map<String, Object>>
    buildRssMessage(RssSource rssSource, SyndEntry entry) {
        StringJoiner joiner = new StringJoiner("\n");
        if (entry.getTitle() != null && !entry.getTitle().isBlank()) {
            joiner.add(String.format("%s", entry.getTitle()));
        }
        if (entry.getDescription() != null && !entry.getDescription().getValue().isBlank()
                && !entry.getTitle().equals(entry.getDescription().getValue())
        ) {
            String description = entry.getDescription().getValue().trim();
            if ("text/html".equals(entry.getDescription().getType())) {
                description = Jsoup.parse(description).text();
            }
            joiner.add("");
            joiner.add(description);
            joiner.add("");
        }
        if (entry.getAuthor() != null && !entry.getAuthor().isBlank()) {
            joiner.add(String.format("(%s)", entry.getAuthor().trim()));
        }
        joiner.add(String.format("RSS:〔%s〕", rssSource.getTitle()));
        List<ArrayMsg> msg = ArrayMsgUtils.builder()
                .text(joiner.toString()).build();

        List<String> msgList = new ArrayList<>();
        msgList.add(ShiroUtils.arrayMsgToCode(msg));
        msgList.add(entry.getLink());
        String webScreenshot;
        try {
            playwrightHandle.navigate(this.page, entry.getLink());
            webScreenshot = Base64.getEncoder().encodeToString(
                    this.page.screenshot(new Page.ScreenshotOptions().setFullPage(true)));
            msgList.add(ShiroUtils.arrayMsgToCode(ArrayMsgUtils.builder()
                    .img("base64://" + webScreenshot).build()));
        } catch (RuntimeException e) {
            log.info(e.toString());
        }
        return ShiroUtils.generateForwardMsg(msgList);
    }
}
