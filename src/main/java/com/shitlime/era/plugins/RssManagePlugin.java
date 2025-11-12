package com.shitlime.era.plugins;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.common.GroupMemberRoleConstants;
import com.shitlime.era.config.EraConfig;
import com.shitlime.era.pojo.dto.RssDTO;
import com.shitlime.era.service.RssManageService;
import com.shitlime.era.utils.EraBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Shiro
@Component
public class RssManagePlugin extends SessionPlugin {
    private static final String REMOVE_SESSION_TAG = "remove";
    private static final String ENABLE_SESSION_TAG = "enable";
    private static final String DISABLE_SESSION_TAG = "disable";
    private static final List<MsgId> recallList = new ArrayList<>();

    @Autowired
    private EraConfig eraConfig;
    @Autowired
    private EraBotUtils eraBotUtils;
    @Autowired
    private RssManageService rssManageService;

    @Override
    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
        // 群组中只有群主/管理/Bot管理才有权限访问Rss管理插件
        if (event.getGroupId() != null &&
            !(GroupMemberRoleConstants.ADMIN.equals(event.getSender().getRole())
                || GroupMemberRoleConstants.OWNER.equals(event.getSender().getRole()))
            && !eraBotUtils.isAdmin(event.getSender().getUserId())
        ) {
            return MESSAGE_IGNORE;
        }

        if (hasSharedSession(event, REMOVE_SESSION_TAG)) {
            // 会话：删除rss
            if (event.getMessage().matches("\\d+")) {
                log.info("删除rss");
                List<ArrayMsg> msg = rssManageService.removeRss(
                        event.getGroupId(), event.getSender().getUserId(),
                        Integer.parseInt(event.getMessage()));
                ActionData<MsgId> actionData = bot.sendMsg(event, msg, true);
                recallList.add(actionData.getData());
            } else if ("|ok".equals(event.getMessage())) {
                log.info("关闭删除rss订阅对话");
                recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId()));
                closeSharedSession(event, REMOVE_SESSION_TAG);
            }
            return MESSAGE_BLOCK;
        } else if (hasSharedSession(event, ENABLE_SESSION_TAG)) {
            // 会话：启用rss
            if (event.getMessage().matches("\\d+")) {
                log.info("启用rss");
                List<ArrayMsg> msg = rssManageService.enableRss(
                        event.getGroupId(), event.getSender().getUserId(),
                        Integer.parseInt(event.getMessage()));
                ActionData<MsgId> actionData = bot.sendMsg(event, msg, true);
                recallList.add(actionData.getData());
            } else if ("|ok".equals(event.getMessage())) {
                log.info("关闭启用rss订阅对话");
                recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId()));
                closeSharedSession(event, ENABLE_SESSION_TAG);
            }
            return MESSAGE_BLOCK;
        } else if (hasSharedSession(event, DISABLE_SESSION_TAG)) {
            // 会话：禁用rss
            if (event.getMessage().matches("\\d+")) {
                log.info("禁用rss");
                List<ArrayMsg> msg = rssManageService.disableRss(
                        event.getGroupId(), event.getSender().getUserId(),
                        Integer.parseInt(event.getMessage()));
                ActionData<MsgId> actionData = bot.sendMsg(event, msg, true);
                recallList.add(actionData.getData());
            } else if ("|ok".equals(event.getMessage())) {
                log.info("关闭禁用rss订阅对话");
                recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId()));
                closeSharedSession(event, DISABLE_SESSION_TAG);
            }
            return MESSAGE_BLOCK;
        } else if (getHelpCmd().equals(event.getMessage())) {
            // 帮助
            return openHelp(bot, event);
        } else if (event.getMessage().matches(getAddCmd())) {
            // 订阅
            Pattern pattern = Pattern.compile(getAddCmd());
            Matcher matcher = pattern.matcher(event.getMessage());
            if (!matcher.find()) {
                return MESSAGE_IGNORE;
            }
            String url = matcher.group(1);
            RssDTO rss = new RssDTO();
            rss.setUrl(url);
            rss.setGroupId(event.getGroupId());
            rss.setUserId(event.getSender().getUserId());

            log.info("{}添加新的rss订阅:{}", event.getSender().getNickname(), url);

            List<ArrayMsg> msg = rssManageService.addRss(rss);

            bot.sendMsg(event, msg, true);
            return MESSAGE_BLOCK;
        } else if (getShowCmd().equals(event.getMessage())) {
            // 展示订阅
            log.info("展示rss订阅");

            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());

            bot.sendMsg(event, msg, true);
            return MESSAGE_BLOCK;
        } else if (getRemoveCmd().equals(event.getMessage())) {
            // 删除订阅
            log.info("打开删除rss订阅对话");
            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());
            ArrayMsg arrayMsg = new ArrayMsg();
            arrayMsg.setType(MsgTypeEnum.text);
            HashMap<String, String> map = new HashMap<>();
            map.put("text", "\n> 输入要删除的编号");
            arrayMsg.setData(map);
            msg.add(arrayMsg);
            ActionData<MsgId> actionData = bot.sendMsg(event, msg, true);
            recallList.add(actionData.getData());
            openSharedSession(event, REMOVE_SESSION_TAG, 110,
                    () -> recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId())));
            return MESSAGE_BLOCK;
        } else if (getEnableCmd().equals(event.getMessage())) {
            // 启用
            log.info("打开启用rss订阅对话");
            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());
            ArrayMsg arrayMsg = new ArrayMsg();
            arrayMsg.setType(MsgTypeEnum.text);
            HashMap<String, String> map = new HashMap<>();
            map.put("text", "\n> 输入要启用的编号");
            arrayMsg.setData(map);
            msg.add(arrayMsg);
            ActionData<MsgId> actionData = bot.sendMsg(event, msg, true);
            recallList.add(actionData.getData());
            openSharedSession(event, ENABLE_SESSION_TAG, 110,
                    () -> recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId())));
            return MESSAGE_BLOCK;
        } else if (getDisableCmd().equals(event.getMessage())) {
            // 禁用
            log.info("打开禁用rss订阅对话");
            List<ArrayMsg> msg = rssManageService
                    .showRss(event.getGroupId(), event.getSender().getUserId());
            ArrayMsg arrayMsg = new ArrayMsg();
            arrayMsg.setType(MsgTypeEnum.text);
            HashMap<String, String> map = new HashMap<>();
            map.put("text", "\n> 输入要禁用的编号");
            arrayMsg.setData(map);
            msg.add(arrayMsg);
            ActionData<MsgId> actionData = bot.sendMsg(event, msg, true);
            recallList.add(actionData.getData());
            openSharedSession(event, DISABLE_SESSION_TAG, 110,
                    () -> recallList.forEach(msgId -> bot.deleteMsg(msgId.getMessageId())));
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    /**
     * 帮助
     * @param bot
     * @param event
     * @return
     */
    private int openHelp(Bot bot, AnyMessageEvent event) {
        log.info("打开rss帮助");

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("　　【RSS插件】");
        joiner.add("by: Shitlime 2024-04-24");
        joiner.add("------ 帮助 ------");
        joiner.add("1.订阅新的RSS：");
        joiner.add(getAddCmd());
        joiner.add("2.查看已订阅RSS：");
        joiner.add(getShowCmd());
        joiner.add("3.删除订阅的RSS：");
        joiner.add(getRemoveCmd());
        joiner.add("4.启用RSS：");
        joiner.add(getEnableCmd());
        joiner.add("5.禁用RSS：");
        joiner.add(getDisableCmd());
        joiner.add("6.查看本帮助：");
        joiner.add(getHelpCmd());
        joiner.add("※以上群组内仅群主/管理有权限");

        bot.sendMsg(event, ArrayMsgUtils.builder()
                .text(joiner.toString()).build(), true);
        return MESSAGE_BLOCK;
    }

    private String getHelpCmd() {
        return String.format("%srss help", eraConfig.getBot().getCmd());
    }

    private String getAddCmd() {
        String perfix = eraConfig.getBot().getCmd();
        perfix = perfix.matches("(?:\\||\\\\)")? "\\"+perfix : perfix;
        return String.format("%srss sub (\\S+)", perfix);
    }

    private String getShowCmd() {
        return String.format("%srss show", eraConfig.getBot().getCmd());
    }

    private String getRemoveCmd() {
        return String.format("%srss rm", eraConfig.getBot().getCmd());
    }

    private String getEnableCmd() {
        return String.format("%srss enable", eraConfig.getBot().getCmd());
    }

    private String getDisableCmd() {
        return String.format("%srss disable", eraConfig.getBot().getCmd());
    }
}
