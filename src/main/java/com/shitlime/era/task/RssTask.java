package com.shitlime.era.task;

import com.mikuac.shiro.common.utils.ArrayMsgUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
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
        if (!tableUtils.isExist(RssSubscriptionMapper.tableName)) {
            return;
        }

        log.info("执行rss订阅更新任务。");

        List<Long> sourceIds = rssSubscriptionMapper.selectAllSourceId();
        List<RssSource> rssSources = rssSourceMapper.selectByIds(sourceIds);

        for (RssSource rssSource : rssSources) {
            String url = rssSource.getUrl();
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));

            if (!rssSource.getLatestLink().equals(feed.getEntries().getFirst().getLink())) {
                for (SyndEntry entry : feed.getEntries()) {
                    if (!rssSource.getLatestLink().equals(entry.getTitle())) {
                        StringJoiner joiner = new StringJoiner("\n");
                        if (entry.getTitle()!=null) {
                            joiner.add(String.format("%s", entry.getTitle()));
                        }
                        if (entry.getLink()!=null) {
                            joiner.add(entry.getLink());
                        }
                        if (rssSource.getTitle()!=null) {
                            joiner.add(String.format("rss:〔%s〕", rssSource.getTitle()));
                        }
                        List<ArrayMsg> msg = ArrayMsgUtils.builder()
                                .text(joiner.toString()).buildList();

                        List<RssSubscription> rssSubscriptions = rssSubscriptionMapper
                                .selectBySourceId(rssSource.getId());
                        for (RssSubscription rssSubscription : rssSubscriptions) {
                            Bot bot = botContainer.robots.get(eraConfig.getBot().getId());
                            if (rssSubscription.getGroupId() != null) {
                                bot.sendGroupMsg(rssSubscription.getGroupId(), msg, true);
                            } else {
                                bot.sendPrivateMsg(rssSubscription.getUserId(), msg, true);
                            }
                        }

                        log.info("{}有新的条目：{}。", feed.getTitle(), entry.getTitle());
                    } else {
                        break;
                    }
                }

                // 发送完订阅后更新数据库
                rssSource.setLatestLink(feed.getEntries().getFirst().getLink());
                rssSource.setLatestTitle(feed.getEntries().getFirst().getTitle());
                rssSource.setFetchTime(LocalDateTime.now());
                rssSourceMapper.fetch(rssSource);
            }
        }
    }
}
