package com.shitlime.era.service;

import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.model.ArrayMsg;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.shitlime.era.mapper.RssSourceMapper;
import com.shitlime.era.mapper.RssSubscriptionMapper;
import com.shitlime.era.pojo.dto.RssDTO;
import com.shitlime.era.pojo.entry.RssSource;
import com.shitlime.era.pojo.entry.RssSubscription;
import com.shitlime.era.utils.TableUtils;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

@Service
public class RssManageService {
    @Autowired
    private TableUtils tableUtils;
    @Autowired
    private RssSubscriptionMapper rssSubscriptionMapper;
    @Autowired
    private RssSourceMapper rssSourceMapper;

    /**
     * 添加 rss 订阅
     *
     * @param rss
     * @return
     */
    @Transactional
    @SneakyThrows(value = {IOException.class, FeedException.class})
    public List<ArrayMsg> addRss(RssDTO rss) {
        checkTableExist();

        // 该用户对象已经订阅过此url
        List<RssSubscription> rssSubscriptions = rssSubscriptionMapper
                .show(rss.getGroupId(), rss.getUserId());
        List<Long> ids = rssSubscriptions.stream().map(RssSubscription::getSourceId).toList();
        List<RssSource> rssSources = rssSourceMapper.selectByIds(ids);

        if (rssSources.stream().anyMatch(r -> rss.getUrl().equals(r.getUrl()))) {
            return ArrayMsgUtils.builder().text("订阅失败，该URL已经订阅过。").buildList();
        }

        RssSource rssSource = rssSourceMapper.selectByUrl(rss.getUrl());
        if (rssSource == null) {
            // 其他用户对象未订阅过此url
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(rss.getUrl())));
            LocalDateTime now = LocalDateTime.now();
            rssSource = new RssSource();
            rssSource.setUrl(rss.getUrl());
            rssSource.setTitle(feed.getTitle());
            rssSource.setLatestLink(feed.getEntries().getFirst().getLink());
            rssSource.setLatestTitle(feed.getEntries().getFirst().getTitle());
            rssSource.setFetchTime(now);
            rssSource.setUpdateTime(now);
            rssSource.setCreateTime(now);
            rssSourceMapper.insert(rssSource);
        }

        LocalDateTime now = LocalDateTime.now();
        RssSubscription rssSubscription = new RssSubscription();
        rssSubscription.setSourceId(rssSource.getId());
        rssSubscription.setGroupId(rss.getGroupId());
        rssSubscription.setUserId(rss.getUserId());
        rssSubscription.setEnable(true);
        rssSubscription.setUpdateTime(now);
        rssSubscription.setCreateTime(now);
        rssSubscriptionMapper.insert(rssSubscription);

        return ArrayMsgUtils.builder()
                .text(String.format("〔%s〕订阅成功。", rssSource.getTitle())).buildList();
    }

    /**
     * 展示 rss 订阅
     * @param groupId
     * @param userId
     * @return
     */
    public List<ArrayMsg> showRss(Long groupId, Long userId) {
        checkTableExist();

        StringJoiner joiner = new StringJoiner("\n");

        List<RssSubscription> rssSubscriptions = rssSubscriptionMapper.show(groupId, userId);
        for (int i = 0; i < rssSubscriptions.size(); i++) {
            RssSubscription rss = rssSubscriptions.get(i);
            RssSource rssSource = rssSourceMapper.selectById(rss.getSourceId());
            joiner.add(String.format("%s.[%s]%s",
                    i + 1, rss.getEnable()? "已启用":"已禁用", rssSource.getTitle()));
        }

        return ArrayMsgUtils.builder().text(joiner.toString()).buildList();
    }

    /**
     * 根据编号删除 rss 订阅
     * @param groupId
     * @param userId
     * @param index
     * @return
     */
    @Transactional
    public List<ArrayMsg> removeRss(Long groupId, Long userId, Integer index) {
        checkTableExist();

        List<RssSubscription> rssList = rssSubscriptionMapper.show(groupId, userId);

        if ( index != null && index > 0 && index <= rssList.size()) {
            RssSubscription removed = rssList.get(index - 1);
            rssSubscriptionMapper.delete(removed.getId());
            // 如果没有其他人订阅，删除对应的 rss source
            RssSource rssSource = rssSourceMapper.selectById(removed.getSourceId());
            List<RssSubscription> result = rssSubscriptionMapper.selectBySourceId(removed.getSourceId());
            if (result == null || result.isEmpty()) {
                rssSourceMapper.delete(removed.getSourceId());
            }
            return ArrayMsgUtils.builder()
                    .text(String.format("成功删除rss〔%s〕\n", rssSource.getTitle()))
                    .text("退出删除模式请发送“|ok”")
                    .buildList();
        }
        return ArrayMsgUtils.builder().text("请检查编号是否错误").buildList();
    }

    /**
     * 启用 rss 订阅
     * @param groupId
     * @param userId
     * @param index
     * @return
     */
    @Transactional
    @SneakyThrows(value = {IOException.class, FeedException.class})
    public List<ArrayMsg> enableRss(Long groupId, Long userId, Integer index) {
        checkTableExist();

        List<RssSubscription> rssList = rssSubscriptionMapper.show(groupId, userId);

        if ( index != null && index > 0 && index <= rssList.size()) {
            RssSubscription rssSubscription = rssList.get(index - 1);
            RssSource rssSource = rssSourceMapper
                    .selectById(rssSubscription.getSourceId());
            SyndFeed feed = new SyndFeedInput().build(
                    new XmlReader(new URL(rssSource.getUrl())));
            rssSource.setFetchTime(LocalDateTime.now());
            rssSource.setLatestTitle(feed.getEntries().getFirst().getTitle());
            rssSource.setLatestLink(feed.getEntries().getFirst().getLink());
            rssSourceMapper.fetch(rssSource);
            rssSubscriptionMapper.enable(rssSubscription.getId());
            return ArrayMsgUtils.builder()
                    .text(String.format("成功启用rss〔%s〕\n", rssSource.getTitle()))
                    .text("退出启用模式请发送“|ok”")
                    .buildList();
        }
        return ArrayMsgUtils.builder().text("请检查编号是否错误").buildList();
    }

    /**
     * 禁用 rss 订阅
     * @param groupId
     * @param userId
     * @param index
     * @return
     */
    @Transactional
    public List<ArrayMsg> disableRss(Long groupId, Long userId, Integer index) {
        checkTableExist();

        List<RssSubscription> rssList = rssSubscriptionMapper.show(groupId, userId);

        if ( index != null && index > 0 && index <= rssList.size()) {
            RssSubscription rssSubscription = rssList.get(index - 1);
            RssSource rssSource = rssSourceMapper.selectById(rssSubscription.getSourceId());
            rssSubscriptionMapper.disable(rssSubscription.getId());
            return ArrayMsgUtils.builder()
                    .text(String.format("成功禁用rss〔%s〕\n", rssSource.getTitle()))
                    .text("退出禁用模式请发送“|ok”")
                    .buildList();
        }
        return ArrayMsgUtils.builder().text("请检查编号是否错误").buildList();
    }

    private void checkTableExist() {
        if (!tableUtils.isExist(RssSubscriptionMapper.tableName)) {
            rssSubscriptionMapper.createTable();
        }
        if (!tableUtils.isExist(RssSourceMapper.tableName)) {
            rssSourceMapper.createTable();
        }
    }
}
