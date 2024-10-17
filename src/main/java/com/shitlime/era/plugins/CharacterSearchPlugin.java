package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.pojo.entry.QuickSearch;
import com.shitlime.era.service.CharacterSearchService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

/**
 * 查字插件
 */
@Slf4j
@Shiro
@Component
public class CharacterSearchPlugin {
    @Autowired
    private CharacterSearchService characterSearchService;

    /**
     * 异步装载数据集
     */
    @PostConstruct
    public void init() {
        log.info("异步装载数据集...");
        
        // 确保 DatasetConfig 类被加载
        try {
            Class.forName("com.shitlime.era.pojo.config.dataset.DatasetConfig");
        } catch (ClassNotFoundException e) {
            log.error("DatasetConfig 类未找到", e);
            return;
        }

        CompletableFuture.runAsync(() -> characterSearchService.loadDatasets());
        log.info("异步装载数据集 完成"); 
    }

    /**
     * 插件帮助菜单
     * @param bot
     * @param event
     * @param matcher
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "^(?:\\?|？)help$")
    public void help(Bot bot, AnyMessageEvent event, Matcher matcher) {
        StringJoiner joiner = new StringJoiner("\n");

        joiner.add("　　【查字插件】");
        joiner.add("by: Shitlime 2024-04-22");
        joiner.add("------ 帮助 ------");
        joiner.add("1.查询所有数据集：");
        joiner.add("?　<关键词>");
        joiner.add("2.查询指定的数据集：");
        joiner.add("?　<数据集id>　<关键词>");
        joiner.add("3.设置快捷查询：");
        joiner.add("[%|🔍]　set　<数据集id>");
        joiner.add("4.快捷查询：");
        joiner.add("[%|🔍]<关键词>");
        joiner.add("5.清除设置的快捷查询：");
        joiner.add("[%|🔍]　clear");
        joiner.add("6.列出所有已装载数据集：");
        joiner.add("?list");

        String msg = joiner.toString();
        bot.sendMsg(event, ArrayMsgUtils.builder().text(msg).build(), true);
    }

    /**
     * 根据数据集 id, keyword 搜索结果
     * @param bot
     * @param event
     * @param matcher
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "^(?:\\?|？)(\\S+)\\s(.+)$")
    public void searchById(Bot bot, AnyMessageEvent event, Matcher matcher) {
        String id = ShiroUtils.escape2(matcher.group(1));
        String keyword = ShiroUtils.escape2(matcher.group(2));

        log.info("根据数据集id={},keyword={}搜索结果", id, keyword);
        List<ArrayMsg> msg = characterSearchService.searchById(id, keyword);

        bot.sendMsg(event, msg, true);
    }

    /**
     * 搜索所有数据集
     * @param bot
     * @param event
     * @param matcher
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "^(?:\\?|？)\\s(.+)$")
    public void searchAll(Bot bot, AnyMessageEvent event, Matcher matcher) {
        String keyword = ShiroUtils.escape2(matcher.group(1));

        log.info("搜索所有数据集");
        List<String> msg = characterSearchService.searchAllDataset(keyword);

        if (msg==null) {
            bot.sendMsg(event, ArrayMsgUtils.builder()
                    .text("目前没有任何数据集").build(), true);
            return;
        }
        List<Map<String, Object>> fwmsg = ShiroUtils.generateForwardMsg(msg);
        bot.sendForwardMsg(event, fwmsg);
    }

    /**
     * 设置快捷查询
     * @param bot
     * @param event
     * @param matcher
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "^(%|🔍)\\sset\\s(\\S+)$")
    public void setQuickSearch(Bot bot, AnyMessageEvent event, Matcher matcher) {
        QuickSearch quickSearch = new QuickSearch();
        quickSearch.setPrefix(ShiroUtils.escape2(matcher.group(1)));
        quickSearch.setDatasetId(ShiroUtils.escape2(matcher.group(2)));
        quickSearch.setGroupId(event.getGroupId());
        quickSearch.setUserId(event.getSender().getUserId());

        log.info("用户{}设置{}快捷查询为{}数据集",
                event.getSender().getNickname(),
                quickSearch.getPrefix(),
                quickSearch.getDatasetId());
        List<ArrayMsg> msg = characterSearchService.setQuickSearch(quickSearch);

        bot.sendMsg(event, msg, true);
    }

    /**
     * 清除自己的所有快捷查询
     * @param bot
     * @param event
     * @param matcher
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "^(%|🔍)\\sclear$")
    public void clearQuickSearch(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Long userId = event.getSender().getUserId();

        log.info("用户{}清除自己的所有快捷查询", event.getSender().getNickname());
        characterSearchService.clearQuickSearch(userId);

        bot.sendMsg(event,
                ArrayMsgUtils.builder().text(
                        String.format("已经清除%s设置的所有快捷查询",
                        event.getSender().getNickname()))
                        .build(), true);
    }

    /**
     * 快捷查询
     * @param bot
     * @param event
     * @param matcher
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "^(%|🔍)(?!\\sset\\s|\\sclear)(.+)$")
    public void quickSearch(Bot bot, AnyMessageEvent event, Matcher matcher) {
        QuickSearch quickSearch = new QuickSearch();
        quickSearch.setPrefix(ShiroUtils.escape2(matcher.group(1)));
        quickSearch.setGroupId(event.getGroupId());
        quickSearch.setUserId(event.getSender().getUserId());
        String keyword = ShiroUtils.escape2(matcher.group(2));

        log.info("{}快捷查询keyword={}", quickSearch.getPrefix(), keyword);
        List<ArrayMsg> msg = characterSearchService.quickSearch(quickSearch, keyword);

        bot.sendMsg(event, msg, true);
    }

    /**
     * 列出所有已装载数据集
     * @param bot
     * @param event
     * @param matcher
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "^(?:\\?|？)list$")
    public void datasetList(Bot bot, AnyMessageEvent event, Matcher matcher) {
        log.info("列出所有已装载数据集");
        List<ArrayMsg> msg = characterSearchService.datasetList();

        bot.sendMsg(event, msg, true);
    }
}
