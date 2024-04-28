package com.shitlime.era.task;

import com.alibaba.fastjson2.JSON;
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
import com.shitlime.era.mapper.RssSourceMapper;
import com.shitlime.era.mapper.RssSubscriptionMapper;
import com.shitlime.era.pojo.entry.RssSource;
import com.shitlime.era.pojo.entry.RssSubscription;
import com.shitlime.era.utils.TableUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Component
public class RssTask {
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

    @Scheduled(cron = "0 1/12 * * * ?")
    @SneakyThrows(value = {IOException.class, FeedException.class})
    public void fetchRss() {
        if (!tableUtils.isExist(RssSubscriptionMapper.tableName)
                || !tableUtils.isExist(RssSourceMapper.tableName)
        ) {
            return;
        }

        log.info("执行rss订阅更新任务。");

        List<Long> sourceIds = rssSubscriptionMapper.selectAllSourceId();
        if (sourceIds==null || sourceIds.isEmpty()) {
            log.info("没有任何启用的rss订阅。");
            return;
        }
        List<RssSource> rssSources = rssSourceMapper.selectByIds(sourceIds);

        for (RssSource rssSource : rssSources) {
            List<String> latestFeed = JSON.parseArray(rssSource.getLatestFeed(), String.class);
            String url = rssSource.getUrl();
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));

            boolean hasUpdate = false;
            for (SyndEntry entry : feed.getEntries()) {
                if (latestFeed.stream().noneMatch(l -> l.equals(entry.getLink()))) {
                    // 如果有新的entry
                    hasUpdate = true;

                    // 构建消息
                    StringJoiner joiner = new StringJoiner("\n");
                    if (entry.getTitle() != null) {
                        joiner.add(String.format("%s", entry.getTitle()));
                    }
                    if (entry.getDescription() != null &&
                        !entry.getTitle().equals(entry.getDescription().getValue())
                    ) {
                        String description = entry.getDescription().getValue().trim();
                        if ("text/html".equals(entry.getDescription().getType())) {
                            description = Jsoup.parse(description).text();
                        }
                        joiner.add("");
                        joiner.add(description);
                        joiner.add("");
                    }
                    joiner.add(String.format("RSS:〔%s〕", rssSource.getTitle()));
                    List<ArrayMsg> msg = ArrayMsgUtils.builder()
                            .text(joiner.toString()).buildList();

                    List<String> msgList = new ArrayList<>();
                    msgList.add(ShiroUtils.arrayMsgToCode(msg));
                    msgList.add(entry.getLink());
                    List<Map<String, Object>> fwmsg = ShiroUtils.generateForwardMsg(msgList);

                    // 给所有订阅者发送消息
                    List<RssSubscription> rssSubscriptions = rssSubscriptionMapper
                            .selectBySourceId(rssSource.getId());
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
        log.info("rss订阅更新任务执行完毕。");
    }
}
